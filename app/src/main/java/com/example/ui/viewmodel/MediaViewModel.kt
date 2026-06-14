package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FavoriteItem
import com.example.data.database.NexPlayDatabase
import com.example.data.database.PlaybackHistory
import com.example.data.database.Playlist
import com.example.data.database.PlaylistItem
import com.example.data.database.VaultItem
import com.example.data.model.AudioItem
import com.example.data.model.FileItem
import com.example.data.model.PhotoItem
import com.example.data.model.VideoItem
import com.example.data.repository.MediaRepository
import com.example.data.repository.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexPlayDatabase.getDatabase(application)
    private val repository = MediaRepository(database.nexPlayDao())

    // --- State Lists ---
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _audios = MutableStateFlow<List<AudioItem>>(emptyList())
    val audios: StateFlow<List<AudioItem>> = _audios.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _currentDirectory = MutableStateFlow<String?>(null)
    val currentDirectory: StateFlow<String?> = _currentDirectory.asStateFlow()

    // --- Dynamic Stats ---
    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0, 0, 0, 1024 * 1024 * 1024))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val largeFiles: StateFlow<List<FileItem>> = _largeFiles.asStateFlow()

    private val _duplicateFiles = MutableStateFlow<List<Pair<FileItem, FileItem>>>(emptyList())
    val duplicateFiles: StateFlow<List<Pair<FileItem, FileItem>>> = _duplicateFiles.asStateFlow()

    // --- Room Observables ---
    private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())
    val vaultItems: StateFlow<List<VaultItem>> = _vaultItems.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<PlaybackHistory>>(emptyList())
    val history: StateFlow<List<PlaybackHistory>> = _history.asStateFlow()

    // --- App Player Controls & Floating Hub States ---
    private val _currentAudio = MutableStateFlow<AudioItem?>(null)
    val currentAudio: StateFlow<AudioItem?> = _currentAudio.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _audioPlaybackSpeed = MutableStateFlow(1.0f)
    val audioPlaybackSpeed: StateFlow<Float> = _audioPlaybackSpeed.asStateFlow()

    private val _audioProgress = MutableStateFlow(0L)
    val audioProgress: StateFlow<Long> = _audioProgress.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _videoPlaybackSpeed = MutableStateFlow(1.0f)
    val videoPlaybackSpeed: StateFlow<Float> = _videoPlaybackSpeed.asStateFlow()

    private val _videoProgress = MutableStateFlow(0L)
    val videoProgress: StateFlow<Long> = _videoProgress.asStateFlow()

    private val _audioBoostPower = MutableStateFlow(1.0f) // 1.0 = normal, max 4.0
    val audioBoostPower: StateFlow<Float> = _audioBoostPower.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0) // 0 = off
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    // Equalizer EQ Presets
    val equalizerPresets = listOf("Normal", "Bass Boost", "Classic", "Electronic", "Jazz", "Pop", "Rock")
    private val _selectedEqualizer = MutableStateFlow("Normal")
    val selectedEqualizer: StateFlow<String> = _selectedEqualizer.asStateFlow()

    // Subtitle displays & Lyrics Display
    private val _lyrics = MutableStateFlow<String>("")
    val lyrics: StateFlow<String> = _lyrics.asStateFlow()

    private val _videoSubtitle = MutableStateFlow<String>("")
    val videoSubtitle: StateFlow<String> = _videoSubtitle.asStateFlow()

    private val _isFloatingVideoActive = MutableStateFlow(false)
    val isFloatingVideoActive: StateFlow<Boolean> = _isFloatingVideoActive.asStateFlow()

    // --- Private Vault PIN and Security Vault ---
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _savedPIN = MutableStateFlow<String?>(null)
    val savedPIN: StateFlow<String?> = _savedPIN.asStateFlow()

    // Timer components
    private var sleepTimer: CountDownTimer? = null

    init {
        // Build mock files in files folder so the app contains stunning files on startup even
        // in fresh environments
        generateSampleAssetsIfEmpty()

        // Sync database flows
        viewModelScope.launch {
            repository.allVaultItems.collectLatest { _vaultItems.value = it }
        }
        viewModelScope.launch {
            repository.allPlaylists.collectLatest { _playlists.value = it }
        }
        viewModelScope.launch {
            repository.allFavorites.collectLatest { _favorites.value = it }
        }
        viewModelScope.launch {
            repository.playbackHistory.collectLatest { _history.value = it }
        }

        // Load saved PIN from SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("nexplay_vault_prefs", Context.MODE_PRIVATE)
        _savedPIN.value = prefs.getString("vault_pin", null)

        // Initial Load of Device media
        refreshAllMedia()
    }

    // --- Media Loading ---
    fun refreshAllMedia() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            // Query Device MediaStore
            val fetchedVideos = repository.fetchVideosFromDevice(context).toMutableList()
            val fetchedAudios = repository.fetchAudiosFromDevice(context).toMutableList()
            val fetchedPhotos = repository.fetchPhotosFromDevice(context).toMutableList()

            // Merge created sample files from filesDir if SQLite sizes/results are empty
            val localFiles = File(context.filesDir, "samples").listFiles()
            localFiles?.forEach { file ->
                val path = file.absolutePath
                val uri = Uri.fromFile(file)
                val ext = file.name.lowercase()
                val size = file.length()
                val id = file.name.hashCode().toLong()

                if (ext.endsWith(".wav") || ext.endsWith(".mp3")) {
                    if (fetchedAudios.none { it.path == path }) {
                        fetchedAudios.add(
                            AudioItem(id, file.name.removeSuffix(".$ext"), "Synth Instrument", "NexPlay Original", path, 120000L, size, uri, System.currentTimeMillis(), "NexPlay")
                        )
                    }
                } else if (ext.endsWith(".mp4") || ext.endsWith(".webm") || ext.endsWith(".3gp")) {
                    if (fetchedVideos.none { it.path == path }) {
                        fetchedVideos.add(
                            VideoItem(id, file.name.removeSuffix(".$ext"), path, 45000L, size, uri, System.currentTimeMillis(), "Samples")
                        )
                    }
                } else if (ext.endsWith(".jpg") || ext.endsWith(".png") || ext.endsWith(".gif")) {
                    if (fetchedPhotos.none { it.path == path }) {
                        fetchedPhotos.add(
                            PhotoItem(id, file.name, path, size, uri, System.currentTimeMillis(), "NexPlay", ext.endsWith(".gif"))
                        )
                    }
                }
            }

            _videos.value = fetchedVideos
            _audios.value = fetchedAudios
            _photos.value = fetchedPhotos

            // Load File Manager states
            browseDirectory(_currentDirectory.value)
            _storageStats.value = repository.analyzeStorage(context)
            _largeFiles.value = repository.getLargeFiles(context)
            _duplicateFiles.value = repository.findDuplicateFiles(context)
        }
    }

    // --- File Browser ---
    fun browseDirectory(path: String?) {
        viewModelScope.launch {
            _currentDirectory.value = path
            _files.value = repository.browseDirectory(getApplication(), path)
        }
    }

    fun deleteFileFromExplorer(path: String) {
        viewModelScope.launch {
            val file = File(path)
            if (file.exists() && file.delete()) {
                refreshAllMedia()
            }
        }
    }

    fun cleanStorageCache() {
        viewModelScope.launch {
            repository.clearCacheAndTempFiles(getApplication())
            refreshAllMedia()
        }
    }

    // --- Music Controls ---
    fun playAudio(audio: AudioItem) {
        _currentAudio.value = audio
        _isAudioPlaying.value = true
        _audioProgress.value = 0L

        // Generate synthetic matching lyrics
        _lyrics.value = """
            [00:01] Welcome to Offline Music Stream
            [00:08] Title: ${audio.title}
            [00:15] Album: ${audio.album}
            [00:22] Powered by NexPlay Engine
            [00:30] Pure offline crystal digital audio
            [00:45] Feel the high bass vibrations
            [01:05] Experience premium performance
            [01:25] Beautiful Material 3 UI design
            [01:40] NexPlay X is built for you
            [01:55] Thank you for listening
        """.trimIndent()

        // Insert history
        viewModelScope.launch {
            repository.addHistory(
                audio.path,
                audio.title,
                audio.artist,
                "AUDIO",
                audio.duration,
                0L
            )
        }
    }

    fun toggleAudioPlayback() {
        _isAudioPlaying.value = !_isAudioPlaying.value
    }

    fun updateAudioProgress(pos: Long) {
        _audioProgress.value = pos
    }

    fun setAudioSpeed(speed: Float) {
        _audioPlaybackSpeed.value = speed
    }

    fun changeEqualizer(preset: String) {
        _selectedEqualizer.value = preset
    }

    fun setAudioBoost(boost: Float) {
        _audioBoostPower.value = boost
    }

    // --- Video Controls ---
    fun playVideo(video: VideoItem) {
        _currentVideo.value = video
        _isVideoPlaying.value = true
        _videoProgress.value = 0L

        // Generate matching dynamic subtitles
        _videoSubtitle.value = "Subtitle: Loading ${video.title} Offline Stream..."

        viewModelScope.launch {
            repository.addHistory(
                video.path,
                video.title,
                "",
                "VIDEO",
                video.duration,
                0L
            )
        }
    }

    fun toggleVideoPlayback() {
        _isVideoPlaying.value = !_isVideoPlaying.value
    }

    fun updateVideoProgress(pos: Long) {
        _videoProgress.value = pos
        // Update subtitle displays based on playback time position
        val seconds = pos / 1000
        _videoSubtitle.value = when {
            seconds < 5 -> "Subtitle: Beginning of cinematic sequence [${videoProgress.value / 1000}s]"
            seconds in 5..15 -> "Subtitle: Hardware accelerator enabled - rendering frame buffers... [${videoProgress.value / 1000}s]"
            seconds in 16..25 -> "Subtitle: Crystal clear playback - aspect boundaries fitted. [${videoProgress.value / 1000}s]"
            seconds in 26..35 -> "Subtitle: Powered by NexPlay Ultra-Engine technology. [${videoProgress.value / 1000}s]"
            else -> "Subtitle: Continuing offline video rendering. [${videoProgress.value / 1000}s]"
        }
    }

    fun setVideoSpeed(speed: Float) {
        _videoPlaybackSpeed.value = speed
    }

    fun toggleFloatingVideo(active: Boolean) {
        _isFloatingVideoActive.value = active
    }

    // --- Sleep Timer ---
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimer?.cancel()

        if (minutes > 0) {
            sleepTimer = object : CountDownTimer(minutes * 60 * 1000L, 60000L) {
                override fun onTick(millisUntilFinished: Long) {
                    _sleepTimerMinutes.value = (millisUntilFinished / 60000).toInt() + 1
                }

                override fun onFinish() {
                    _sleepTimerMinutes.value = 0
                    _isAudioPlaying.value = false
                    _isVideoPlaying.value = false
                }
            }.start()
        }
    }

    // --- Playlists & Favorites ---
    fun addTrackToPlaylist(playlistId: Int, audio: AudioItem) {
        viewModelScope.launch {
            repository.addPlaylistItem(
                PlaylistItem(
                    playlistId = playlistId,
                    mediaPath = audio.path,
                    mediaTitle = audio.title,
                    mediaArtist = audio.artist,
                    mediaDuration = audio.duration,
                    mediaType = "AUDIO"
                )
            )
        }
    }

    fun createAndAddPlaylist(name: String, audio: AudioItem?) {
        viewModelScope.launch {
            val pId = repository.createPlaylist(name)
            if (audio != null) {
                addTrackToPlaylist(pId.toInt(), audio)
            }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun toggleFavorite(path: String, type: String, isFav: Boolean) {
        viewModelScope.launch {
            if (isFav) {
                repository.removeFavorite(path)
            } else {
                repository.addFavorite(path, type)
            }
        }
    }

    // --- Private Vault logic ---
    fun setupPIN(pin: String) {
        val prefs = getApplication<Application>().getSharedPreferences("nexplay_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("vault_pin", pin).apply()
        _savedPIN.value = pin
        _isVaultUnlocked.value = true
    }

    fun unlockVault(pin: String): Boolean {
        return if (pin == _savedPIN.value) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun hideMediaToVault(path: String, type: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val success = repository.moveFileToVault(context, path, type)
            if (success) {
                refreshAllMedia()
            }
        }
    }

    fun unhideMediaFromVault(item: VaultItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val success = repository.restoreFileFromVault(context, item)
            if (success) {
                refreshAllMedia()
            }
        }
    }

    // --- Premium Tools (Editor & Converter) ---
    fun convertVideoToMP3(videoPath: String, title: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val samplesDir = File(context.filesDir, "samples")
            if (!samplesDir.exists()) samplesDir.mkdirs()

            val audioFile = File(samplesDir, "Transcoded_$title.mp3")
            val success = repository.convertVideoToAudio(videoPath, audioFile.absolutePath)
            if (success) {
                refreshAllMedia()
            }
        }
    }

    fun trimMedia(sourcePath: String, title: String, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val samplesDir = File(context.filesDir, "samples")
            if (!samplesDir.exists()) samplesDir.mkdirs()

            val ext = File(sourcePath).extension
            val targetFile = File(samplesDir, "Trimmed_${System.currentTimeMillis()}.$ext")
            val success = repository.trimAudioVideo(sourcePath, targetFile.absolutePath, startMs, endMs)
            if (success) {
                refreshAllMedia()
            }
        }
    }


    // --- File Generation (To automatically populate emulator directories with beautiful files) ---
    private fun generateSampleAssetsIfEmpty() {
        val context = getApplication<Application>()
        val samplesDir = File(context.filesDir, "samples")
        if (!samplesDir.exists()) {
            samplesDir.mkdirs()
        }

        // Generate audio files (using wave synthesizers!)
        val noteC = File(samplesDir, "Acoustic_Calm_Guitar.wav")
        if (!noteC.exists()) {
            generateSampleWav(noteC, 261.63, 10.0) // C4 note (10s soft loop)
        }

        val noteE = File(samplesDir, "Midnight_Synth_Harmonics.wav")
        if (!noteE.exists()) {
            generateSampleWav(noteE, 329.63, 15.0) // E4 note (15s deep chime)
        }

        val noteA = File(samplesDir, "Retro_Chiptune_Bass.wav")
        if (!noteA.exists()) {
            generateSampleWav(noteA, 110.00, 8.0) // A2 bass line
        }

        // Generate video file simulator (which is just a small mock data container file we'll scan)
        val sampleVideo1 = File(samplesDir, "NexVora_Lab_Intro_Cinematic.3gp")
        if (!sampleVideo1.exists()) {
            try {
                FileOutputStream(sampleVideo1).use { fos ->
                    // Write dummy MP4-header stream data
                    fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70, 0x33, 0x67, 0x70, 0x34))
                    for (i in 0..8000) {
                        fos.write(byteArrayOf(0x01, 0x02, 0x03, 0x04))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val sampleVideo2 = File(samplesDir, "Nature_Waves_Slowmotion.3gp")
        if (!sampleVideo2.exists()) {
            try {
                FileOutputStream(sampleVideo2).use { fos ->
                    fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70, 0x33, 0x67, 0x70, 0x34))
                    for (i in 0..15000) {
                        fos.write(byteArrayOf(0x05, 0x06, 0x07, 0x08))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Generate text files
        val sampleText = File(samplesDir, "ReadMe_NexPlayX.txt")
        if (!sampleText.exists()) {
            try {
                FileWriter(sampleText).use { writer ->
                    writer.write("""
                        === NexPlay X ===
                        Play Everything. Anywhere. Offline.
                        
                        Developed by: Prince AR Abdur Rahman
                        Published by: NexVora Lab's Ofc
                        Version: 1.0.0
                        
                        This is your primary storage space for saving offline audio tracks, local recordings, private security caches, and photos. Use our File Manager to clean unnecessary duplicate cache chunks!
                    """.trimIndent())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Generate duplicate test file (to test duplicate scanner)
        val duplicateText = File(samplesDir, "ReadMe_NexPlayX_Duplicate_Copy.txt")
        if (!duplicateText.exists()) {
            try {
                FileWriter(duplicateText).use { writer ->
                    writer.write("""
                        === NexPlay X ===
                        Play Everything. Anywhere. Offline.
                        
                        Developed by: Prince AR Abdur Rahman
                        Published by: NexVora Lab's Ofc
                        Version: 1.0.0
                        
                        This is your primary storage space for saving offline audio tracks, local recordings, private security caches, and photos. Use our File Manager to clean unnecessary duplicate cache chunks!
                    """.trimIndent())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateSampleWav(file: File, freq: Double, durationSec: Double) {
        try {
            val sampleRate = 8000
            val numSamples = (sampleRate * durationSec).toInt()
            val dataSize = numSamples * 2
            val totalSize = 36 + dataSize

            val header = ByteArray(44)
            header[0] = 'R'.toByte()
            header[1] = 'I'.toByte()
            header[2] = 'F'.toByte()
            header[3] = 'F'.toByte()
            header[4] = (totalSize and 0xff).toByte()
            header[5] = ((totalSize shr 8) and 0xff).toByte()
            header[6] = ((totalSize shr 16) and 0xff).toByte()
            header[7] = ((totalSize shr 24) and 0xff).toByte()
            header[8] = 'W'.toByte()
            header[9] = 'A'.toByte()
            header[10] = 'V'.toByte()
            header[11] = 'E'.toByte()
            header[12] = 'f'.toByte()
            header[13] = 'm'.toByte()
            header[14] = 't'.toByte()
            header[15] = ' '.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM
            header[21] = 0
            header[22] = 1 // Mono
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            val byteRate = sampleRate * 2
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2
            header[33] = 0
            header[34] = 16
            header[35] = 0
            header[36] = 'd'.toByte()
            header[37] = 'a'.toByte()
            header[38] = 't'.toByte()
            header[39] = 'a'.toByte()
            header[40] = (dataSize and 0xff).toByte()
            header[41] = ((dataSize shr 8) and 0xff).toByte()
            header[42] = ((dataSize shr 16) and 0xff).toByte()
            header[43] = ((dataSize shr 24) and 0xff).toByte()

            FileOutputStream(file).use { fos ->
                fos.write(header)
                val byteBuffer = ByteArray(2)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Synthesize soft beautiful sine scale curve melody
                    val dynamicFreq = if (i > numSamples / 2) freq * 1.25 else freq
                    val value = (Math.sin(2.0 * Math.PI * dynamicFreq * t) * 16383).toInt()
                    byteBuffer[0] = (value and 0xff).toByte()
                    byteBuffer[1] = ((value shr 8) and 0xff).toByte()
                    fos.write(byteBuffer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
