package com.pgerasymchuk.mediaplayer.data

import com.pgerasymchuk.mediaplayer.data.entity.MediaFileEntity
import com.pgerasymchuk.mediaplayer.data.entity.PlaylistEntity
import com.pgerasymchuk.mediaplayer.data.model.MediaFile
import com.pgerasymchuk.mediaplayer.data.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun mediaFileEntity_mapsAllFields() {
        val entity = MediaFileEntity(
            id = 7,
            playlistId = 3,
            name = "song.mp3",
            path = "content://media/42",
            isUrl = false
        )

        assertEquals(MediaFile(7, "song.mp3", "content://media/42", false), entity.toModel())
    }

    @Test
    fun playlistEntity_keepsMediaOrderAndVisibility() {
        val entity = PlaylistEntity(id = 3, name = "Road trip", isVisible = true)
        val files = listOf(
            MediaFileEntity(1, 3, "a.mp3", "content://a", false),
            MediaFileEntity(2, 3, "live.m3u8", "https://radio.example/live.m3u8", true)
        )

        val playlist = entity.toModel(files)

        assertEquals(3L, playlist.id)
        assertEquals("Road trip", playlist.name)
        assertTrue(playlist.isVisible)
        assertEquals(listOf(1L, 2L), playlist.items.map { it.id })
        assertTrue(playlist.items[1].isUrl)
    }

    @Test
    fun playlistModel_roundTripsThroughEntity() {
        val playlist = Playlist(id = 5, name = "Focus", isVisible = false)

        assertEquals(PlaylistEntity(5, "Focus", false), playlist.toEntity())
    }

    @Test
    fun playlistModel_itemsAreMutableAfterMapping() {
        val playlist = PlaylistEntity(1, "Empty").toModel(emptyList())

        playlist.items.add(MediaFile(10, "new.mp3", "content://new"))

        assertEquals(1, playlist.items.size)
    }

    @Test
    fun displayName_usesLastPathSegment() {
        assertEquals("track.mp3", displayNameFromUrl("https://cdn.example.com/music/track.mp3"))
    }

    @Test
    fun displayName_dropsQueryFragmentAndTrailingSlash() {
        assertEquals("live.m3u8", displayNameFromUrl("https://stream.example.com/hls/live.m3u8?token=abc#t=10"))
        assertEquals("radio", displayNameFromUrl("https://stream.example.com/radio/"))
    }

    @Test
    fun displayName_fallsBackToHostOrInput() {
        assertEquals("example.com", displayNameFromUrl("https://example.com/"))
        assertEquals("stream", displayNameFromUrl("  stream  "))
    }
}
