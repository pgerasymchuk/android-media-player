package com.pgerasymchuk.mediaplayer.data.model

data class MediaFile(
    val id: Long = 0,
    val name: String,
    val path: String,
    val isUrl: Boolean = false
)