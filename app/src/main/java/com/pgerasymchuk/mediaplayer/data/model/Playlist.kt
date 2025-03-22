package com.pgerasymchuk.mediaplayer.data.model

data class Playlist(
    val id: Long = 0,
    var name: String,
    val items: MutableList<MediaFile> = mutableListOf(),
    var isVisible: Boolean = false
)