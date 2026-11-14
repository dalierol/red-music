package com.assiastudio.redmusic

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val contentUri: android.net.Uri,
    val duration: Int,  // Şarkı süresi (milisaniye)
    val artworkUri: Uri?
)