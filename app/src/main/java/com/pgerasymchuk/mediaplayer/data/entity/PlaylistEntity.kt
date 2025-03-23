package com.pgerasymchuk.mediaplayer.data.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "is_visible") var isVisible: Boolean = false
)