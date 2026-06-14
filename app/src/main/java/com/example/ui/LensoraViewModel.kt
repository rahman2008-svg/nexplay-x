package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.CustomPresetEntity
import com.example.data.database.LensoraDatabase
import com.example.data.database.ProjectEntity
import com.example.data.repository.LensoraRepository
import com.example.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class LensoraViewModel(
    application: Application,
    private val repository: LensoraRepository
) : AndroidViewModel(application) {

    // Active image states
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    private val _photoState = MutableStateFlow(PhotoState())
    val photoState: StateFlow<PhotoState> = _photoState.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    // Canva Lite states
    private val _canvaElements = MutableStateFlow<List<CanvaElement>>(emptyList())
    val canvaElements: StateFlow<List<CanvaElement>> = _canvaElements.asStateFlow()

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    private val _canvaBackgroundHex = MutableStateFlow("#120224")
    val canvaBackgroundHex: StateFlow<String> = _canvaBackgroundHex.asStateFlow()

    // Screen states
    private val _isCanvaMode = MutableStateFlow(false)
    val isCanvaMode: StateFlow<Boolean> = _isCanvaMode.asStateFlow()

    // App alert states
    private val _errorAlert = MutableStateFlow<String?>(null)
    val errorAlert: StateFlow<String?> = _errorAlert.asStateFlow()

    private val _activeEditingTimeStart = MutableStateFlow(System.currentTimeMillis())

    // Database states
    val projectsList: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetsList: StateFlow<List<CustomPresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(AnalyticsEntityState())
    val stats: StateFlow<AnalyticsEntityState> = _stats.asStateFlow()

    init {
        loadSampleImage()
        loadStats()
    }

    private fun loadSampleImage() {
        // Create an elegant offline abstract gradient landscape bitmap to edit immediately
        val size = 800
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()

        // Create deep rich gradients to showcase Lightroom/Snapseed filters
        val gradient = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(android.graphics.Color.DKGRAY, android.graphics.Color.BLUE, android.graphics.Color.RED),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Draw portrait face approximation (circle with highlight glow)
        paint.shader = null
        paint.color = android.graphics.Color.parseColor("#FFD54F") // warm skin glow
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2.2f, size * 0.22f, paint)

        // Eyes
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f - 40f, size / 2.2f - 20f, 15f, paint)
        canvas.drawCircle(size / 2f + 40f, size / 2.2f - 20f, 15f, paint)
        paint.color = android.graphics.Color.BLACK
        canvas.drawCircle(size / 2f - 40f, size / 2.2f - 20f, 7f, paint)
        canvas.drawCircle(size / 2f + 40f, size / 2.2f - 20f, 7f, paint)

        // Smile / Teeth
        paint.color = android.graphics.Color.parseColor("#FFF59D") // slightly yellow path to demonstrate whitening
        canvas.drawRoundRect(size / 2f - 50f, size / 2.2f + 50f, size / 2f + 50f, size / 2.2f + 70f, 10f, 10f, paint)

        _originalBitmap.value = bitmap
        _editedBitmap.value = bitmap
    }

    fun loadStats() {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            val dbStats = repository.getAnalytics()
            _stats.value = AnalyticsEntityState(
                totalProjects = dbStats.totalProjects,
                totalExports = dbStats.totalExports,
                mostUsedFilter = dbStats.mostUsedFilter,
                totalEditingTimeSeconds = dbStats.totalEditingTimeSeconds,
                storageUsageBytes = dbStats.storageUsageBytes
            )
        }
    }

    fun setImageBitmap(bitmap: Bitmap, fromUri: Uri? = null) {
        _isCanvaMode.value = false
        _selectedUri.value = fromUri
        _originalBitmap.value = bitmap
        _photoState.value = PhotoState() // Reset state parameters
        applyProcessingStack()
    }

    fun loadBitmapFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    setImageBitmap(bitmap, uri)
                } else {
                    _errorAlert.value = "Unable to process the selected image."
                }
            } catch (e: Exception) {
                _errorAlert.value = "Failed to load image: ${e.localizedMessage}"
                LensoraLog.e("Failed loading Uri image", e)
            }
        }
    }

    fun updatePhotoState(updater: (PhotoState) -> PhotoState) {
        _photoState.value = updater(_photoState.value)
        applyProcessingStack()
    }

    fun resetPhotoState() {
        _photoState.value = PhotoState()
        applyProcessingStack()
    }

    fun triggerAutoEnhance() {
        val original = _originalBitmap.value ?: return
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            val autoState = ImageProcessor.getAutoEnhancedState(original)
            _photoState.value = autoState
            applyProcessingStack()
        }
    }

    fun selectPreset(filterName: String) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            _photoState.value = ImageProcessor.getFilterPreset(filterName)
            applyProcessingStack()
            repository.updateMostUsedFilter(filterName)
            loadStats()
        }
    }

    private fun applyProcessingStack() {
        val original = _originalBitmap.value ?: return
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            try {
                val processed = ImageProcessor.processBitmap(original, _photoState.value)
                _editedBitmap.value = processed
            } catch (e: Exception) {
                _errorAlert.value = "Error updating layers: ${e.localizedMessage}"
                LensoraLog.e("Image processing crashed", e)
            }
        }
    }

    fun dismissError() {
        _errorAlert.value = null
    }

    // Custom Preset actions
    fun saveCustomPreset(presetName: String) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            val state = _photoState.value
            val entity = CustomPresetEntity(
                name = presetName,
                brightness = state.brightness,
                contrast = state.contrast,
                saturation = state.saturation,
                exposure = state.exposure,
                temperature = state.temperature,
                tint = state.tint,
                vibrance = state.vibrance,
                vignette = state.vignette,
                sharpness = state.sharpness,
                filterType = "Custom"
            )
            repository.savePreset(entity)
            applyProcessingStack()
        }
    }

    fun applyCustomPreset(preset: CustomPresetEntity) {
        _photoState.value = PhotoState(
            brightness = preset.brightness,
            contrast = preset.contrast,
            saturation = preset.saturation,
            exposure = preset.exposure,
            temperature = preset.temperature,
            tint = preset.tint,
            vibrance = preset.vibrance,
            vignette = preset.vignette,
            sharpness = preset.sharpness,
            chosenFilter = preset.name
        )
        applyProcessingStack()
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            repository.deletePreset(id)
        }
    }

    // Canva section
    fun selectTemplate(template: CanvaTemplate) {
        _isCanvaMode.value = true
        _canvaBackgroundHex.value = template.backgroundHex
        _canvaElements.value = template.elements
        _selectedElementId.value = null
    }

    fun initEmptyCanva(backgroundHex: String = "#120224") {
        _isCanvaMode.value = true
        _canvaBackgroundHex.value = backgroundHex
        _canvaElements.value = emptyList()
        _selectedElementId.value = null
    }

    fun addTextCanvaElement(text: String, colorHex: String = "#FFFFFF", font: String = "Display Bold") {
        val element = CanvaElement.Text(
            x = 100f,
            y = 200f,
            text = text,
            colorHex = colorHex,
            fontFamily = font
        )
        _canvaElements.value = _canvaElements.value + element
    }

    fun addStickerCanvaElement(emoji: String) {
        val element = CanvaElement.Sticker(
            x = 150f,
            y = 150f,
            emoji = emoji
        )
        _canvaElements.value = _canvaElements.value + element
    }

    fun addShapeCanvaElement(shapeType: String, colorHex: String = "#00E5FF") {
        val element = CanvaElement.Shape(
            x = 100f,
            y = 100f,
            shapeType = shapeType,
            colorHex = colorHex
        )
        _canvaElements.value = _canvaElements.value + element
    }

    fun selectCanvaElement(id: String?) {
        _selectedElementId.value = id
    }

    fun updateSelectedElementPosition(dx: Float, dy: Float) {
        val id = _selectedElementId.value ?: return
        _canvaElements.value = _canvaElements.value.map { elem ->
            if (elem.id == id) {
                elem.updatePosition(elem.x + dx, elem.y + dy)
            } else elem
        }
    }

    fun updateSelectedElementScale(scaleFactor: Float) {
        val id = _selectedElementId.value ?: return
        _canvaElements.value = _canvaElements.value.map { elem ->
            if (elem.id == id) {
                val resolvedScale = (elem.scale * scaleFactor).coerceIn(0.2f, 4f)
                elem.updateScale(resolvedScale)
            } else elem
        }
    }

    fun updateSelectedElementRotation(rotationDegrees: Float) {
        val id = _selectedElementId.value ?: return
        _canvaElements.value = _canvaElements.value.map { elem ->
            if (elem.id == id) {
                elem.updateRotation((elem.rotation + rotationDegrees) % 360f)
            } else elem
        }
    }

    fun deleteSelectedElement() {
        val id = _selectedElementId.value ?: return
        _canvaElements.value = _canvaElements.value.filter { it.id != id }
        _selectedElementId.value = null
    }

    // Saves current photo parameters or Canva elements as a Draft project
    fun saveProjectDraft(name: String) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            val stateJson = if (_isCanvaMode.value) {
                // Simple representation of Canva elements
                _canvaElements.value.joinToString(separator = ";") { elem ->
                    when (elem) {
                        is CanvaElement.Text -> "text,${elem.text.replace(",", " ")},${elem.colorHex},${elem.fontFamily},${elem.x},${elem.y},${elem.scale}"
                        is CanvaElement.Sticker -> "sticker,${elem.emoji},${elem.x},${elem.y},${elem.scale}"
                        is CanvaElement.Shape -> "shape,${elem.shapeType},${elem.colorHex},${elem.x},${elem.y},${elem.scale}"
                    }
                }
            } else {
                // Serialization of adjust tags
                val state = _photoState.value
                "adjust,${state.brightness},${state.contrast},${state.saturation},${state.exposure},${state.temperature},${state.tint},${state.vignette},${state.sharpness},${state.portraitBlur},${state.chosenFilter}"
            }

            val project = ProjectEntity(
                name = name,
                isCanvaDesign = _isCanvaMode.value,
                imagePath = _selectedUri.value?.toString(),
                editorStateJson = stateJson,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProject(project)
            repository.incrementTotalProjects()
            loadStats()
        }
    }

    fun restoreDraft(project: ProjectEntity, context: Context) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            _isCanvaMode.value = project.isCanvaDesign
            if (project.isCanvaDesign) {
                val lines = project.editorStateJson.split(";")
                val list = mutableListOf<CanvaElement>()
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        when (parts[0]) {
                            "text" -> {
                                list.add(CanvaElement.Text(
                                    text = parts[1], colorHex = parts[2], fontFamily = parts[3],
                                    x = parts[4].toFloatOrNull() ?: 100f, y = parts[5].toFloatOrNull() ?: 200f,
                                    scale = parts.getOrNull(6)?.toFloatOrNull() ?: 1f
                                ))
                            }
                            "sticker" -> {
                                list.add(CanvaElement.Sticker(
                                    emoji = parts[1],
                                    x = parts[2].toFloatOrNull() ?: 150f, y = parts[3].toFloatOrNull() ?: 150f,
                                    scale = parts.getOrNull(4)?.toFloatOrNull() ?: 1f
                                ))
                            }
                            "shape" -> {
                                list.add(CanvaElement.Shape(
                                    shapeType = parts[1], colorHex = parts[2],
                                    x = parts[3].toFloatOrNull() ?: 100f, y = parts[4].toFloatOrNull() ?: 100f,
                                    scale = parts.getOrNull(5)?.toFloatOrNull() ?: 1f
                                ))
                            }
                        }
                    }
                }
                _canvaElements.value = list
            } else {
                val parts = project.editorStateJson.split(",")
                if (parts.getOrNull(0) == "adjust" && parts.size >= 11) {
                    _photoState.value = PhotoState(
                        brightness = parts[1].toFloatOrNull() ?: 0f,
                        contrast = parts[2].toFloatOrNull() ?: 0f,
                        saturation = parts[3].toFloatOrNull() ?: 0f,
                        exposure = parts[4].toFloatOrNull() ?: 0f,
                        temperature = parts[5].toFloatOrNull() ?: 0f,
                        tint = parts[6].toFloatOrNull() ?: 0f,
                        vignette = parts[10].toFloatOrNull() ?: 0f,
                        sharpness = parts[8].toFloatOrNull() ?: 0f,
                        portraitBlur = parts[9].toFloatOrNull() ?: 0f,
                        chosenFilter = parts[10]
                    )
                }
                // Try restoring bitmap
                project.imagePath?.let { uriStr ->
                    try {
                        val uri = Uri.parse(uriStr)
                        loadBitmapFromUri(uri, context)
                    } catch (e: Exception) {
                        loadSampleImage() // fall back
                    }
                } ?: run {
                    loadSampleImage()
                }
            }
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch(lensoraCoroutineExceptionHandler) {
            repository.deleteProject(id)
        }
    }

    // export image method representing actual compilation activity
    fun triggerExportAction(format: String, quality: String): ByteArray? {
        val bitmap = _editedBitmap.value ?: return null
        return try {
            val bytes = ImageProcessor.exportToFormat(bitmap, format, quality)
            viewModelScope.launch {
                repository.incrementTotalExports()
                repository.updateStorageUsage(bytes.size.toLong())
                loadStats()
            }
            bytes
        } catch (e: Exception) {
            _errorAlert.value = "Export failed: ${e.localizedMessage}"
            LensoraLog.e("Failed to encode exported file", e)
            null
        }
    }

    // Record time metrics when navigating away or on pause
    fun flushTimeStats() {
        val current = System.currentTimeMillis()
        val deltaSec = (current - _activeEditingTimeStart.value) / 1000
        if (deltaSec > 5) {
            viewModelScope.launch {
                repository.addEditingTime(deltaSec)
                loadStats()
            }
        }
        _activeEditingTimeStart.value = current
    }
}

data class AnalyticsEntityState(
    val totalProjects: Int = 0,
    val totalExports: Int = 0,
    val mostUsedFilter: String = "None",
    val totalEditingTimeSeconds: Long = 0,
    val storageUsageBytes: Long = 0
)

class LensoraViewModelFactory(
    private val application: Application,
    private val repository: LensoraRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LensoraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LensoraViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
