package com.example.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val uri: Uri,
    val addedDate: Long,
    val folder: String = ""
)

data class AudioItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val uri: Uri,
    val addedDate: Long,
    val folder: String = ""
)

data class PhotoItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val uri: Uri,
    val addedDate: Long,
    val folder: String = "",
    val isGif: Boolean = false
)

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val isImage: Boolean = false,
    val lastModified: Long = 0L
)
