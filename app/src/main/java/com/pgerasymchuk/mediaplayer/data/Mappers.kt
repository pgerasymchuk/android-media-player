package com.pgerasymchuk.mediaplayer.data

import com.pgerasymchuk.mediaplayer.data.entity.MediaFileEntity
import com.pgerasymchuk.mediaplayer.data.entity.PlaylistEntity
import com.pgerasymchuk.mediaplayer.data.model.MediaFile
import com.pgerasymchuk.mediaplayer.data.model.Playlist

fun MediaFileEntity.toModel() = MediaFile(id, name, path, isUrl)

fun PlaylistEntity.toModel(mediaFiles: List<MediaFileEntity>) = Playlist(
    id = id,
    name = name,
    items = mediaFiles.map { it.toModel() }.toMutableList(),
    isVisible = isVisible
)

fun Playlist.toEntity() = PlaylistEntity(id, name, isVisible)

/**
 * Builds a readable title for a media URL: the last path segment without
 * query string or fragment, falling back to the host or the URL itself.
 */
fun displayNameFromUrl(url: String): String {
    val trimmed = url.trim()
        .substringBefore('#')
        .substringBefore('?')
        .trimEnd('/')
    val name = trimmed.substringAfterLast('/')
    return name.ifEmpty { url.trim() }
}
