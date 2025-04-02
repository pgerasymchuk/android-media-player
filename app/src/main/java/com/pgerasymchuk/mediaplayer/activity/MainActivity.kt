package com.pgerasymchuk.mediaplayer.activity

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.util.concurrent.ListenableFuture
import com.pgerasymchuk.mediaplayer.R
import com.pgerasymchuk.mediaplayer.adapter.PlaylistAdapter
import com.pgerasymchuk.mediaplayer.data.AppDatabase
import com.pgerasymchuk.mediaplayer.data.PlaylistDao
import com.pgerasymchuk.mediaplayer.data.displayNameFromUrl
import com.pgerasymchuk.mediaplayer.data.entity.MediaFileEntity
import com.pgerasymchuk.mediaplayer.data.entity.PlaylistEntity
import com.pgerasymchuk.mediaplayer.data.model.MediaFile
import com.pgerasymchuk.mediaplayer.data.model.Playlist
import com.pgerasymchuk.mediaplayer.data.toEntity
import com.pgerasymchuk.mediaplayer.data.toModel
import com.pgerasymchuk.mediaplayer.databinding.ActivityMainBinding
import com.pgerasymchuk.mediaplayer.service.MediaPlaybackService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var player: MediaController? = null
    private lateinit var binding: ActivityMainBinding
    private val playlists = mutableListOf<Playlist>()
    private lateinit var playlistAdapter: PlaylistAdapter
    private var pendingPlaylistPosition = -1
    private lateinit var database: AppDatabase
    private lateinit var playlistDao: PlaylistDao

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { granted -> !granted }) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val position = pendingPlaylistPosition
        pendingPlaylistPosition = -1
        if (result.resultCode == RESULT_OK && uri != null && position in playlists.indices) {
            lifecycleScope.launch {
                addMediaToPlaylist(position, uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playerView.useController = true

        database = AppDatabase.getDatabase(this)
        playlistDao = database.playlistDao()

        playlistAdapter = PlaylistAdapter(
            playlists,
            { mediaFile: MediaFile -> playMedia(mediaFile) },
            { playlistPos, mediaPos ->
                lifecycleScope.launch {
                    deleteMediaFromPlaylist(playlistPos, mediaPos)
                }
            },
            { position -> showEditPlaylistDialog(position) },
            { position ->
                lifecycleScope.launch {
                    deletePlaylist(position)
                }
            },
            { position -> togglePlaylistVisibility(position) },
            { position -> shufflePlaylist(position) }
        )
        binding.playlistRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.playlistRecyclerView.adapter = playlistAdapter

        binding.addPlaylistButton.setOnClickListener {
            lifecycleScope.launch {
                addNewPlaylist()
            }
        }

        binding.radioButton.setOnClickListener {
            startActivity(Intent(this, RadioActivity::class.java))
        }

        requestStoragePermission()
        loadPlaylistsFromDatabase()
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            if (future.isCancelled) return@addListener
            player = future.get()
            binding.playerView.player = player
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        binding.playerView.player = null
        player = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    private fun loadPlaylistsFromDatabase() {
        lifecycleScope.launch {
            playlists.clear()
            val playlistEntities = playlistDao.getAllPlaylists()
            playlistEntities.forEach { playlistEntity ->
                val mediaFiles = playlistDao.getMediaFilesForPlaylist(playlistEntity.id)
                playlists.add(playlistEntity.toModel(mediaFiles))
            }
            playlistAdapter.notifyDataSetChanged()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                requestPermissionLauncher.launch(permissions)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private suspend fun addNewPlaylist() {
        val playlistEntity = PlaylistEntity(name = "Playlist ${playlists.size + 1}")
        val id = playlistDao.insertPlaylist(playlistEntity)
        playlists.add(Playlist(id, playlistEntity.name))
        playlistAdapter.notifyItemInserted(playlists.size - 1)
    }

    private suspend fun deletePlaylist(position: Int) {
        val playlist = playlists.getOrNull(position) ?: return
        playlistDao.deletePlaylist(playlist.id)
        playlists.removeAt(position)
        playlistAdapter.notifyItemRemoved(position)
    }

    private suspend fun deleteMediaFromPlaylist(playlistPosition: Int, mediaPosition: Int) {
        val playlist = playlists.getOrNull(playlistPosition) ?: return
        val mediaFile = playlist.items.getOrNull(mediaPosition) ?: return
        playlistDao.deleteMediaFile(playlist.id, mediaFile.id)
        playlist.items.removeAt(mediaPosition)
        playlistAdapter.notifyItemChanged(playlistPosition)
    }

    private fun togglePlaylistVisibility(position: Int) {
        val playlist = playlists[position]
        playlist.isVisible = !playlist.isVisible
        lifecycleScope.launch {
            playlistDao.updatePlaylist(playlist.toEntity())
            playlistAdapter.notifyItemChanged(position)
        }
    }

    private fun shufflePlaylist(position: Int) {
        val playlist = playlists[position]
        if (playlist.items.isEmpty()) return
        playlist.items.shuffle()
        playlistAdapter.notifyItemChanged(position)
        playPlaylist(position, 0)
    }

    private fun showEditPlaylistDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_playlist, null)
        val playlistNameInput = dialogView.findViewById<EditText>(R.id.playlistNameInput)
        val urlInput = dialogView.findViewById<EditText>(R.id.urlInput)
        val addUrlButton = dialogView.findViewById<Button>(R.id.addUrlButton)
        val addFileButton = dialogView.findViewById<Button>(R.id.addFileButton)

        val playlist = playlists[position]
        playlistNameInput.setText(playlist.name)

        val dialog = builder.setTitle("Edit Playlist")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = playlistNameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    playlist.name = newName
                    lifecycleScope.launch {
                        playlistDao.updatePlaylist(playlist.toEntity())
                        playlistAdapter.notifyItemChanged(position)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        addUrlButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                lifecycleScope.launch {
                    addUrlToPlaylist(position, url)
                }
                urlInput.text.clear()
            }
        }

        addFileButton.setOnClickListener {
            launchMediaPicker(position)
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun addUrlToPlaylist(position: Int, url: String) {
        val playlist = playlists.getOrNull(position) ?: return
        val mediaFileEntity = MediaFileEntity(
            playlistId = playlist.id,
            name = displayNameFromUrl(url),
            path = url,
            isUrl = true
        )
        val id = playlistDao.insertMediaFile(mediaFileEntity)
        playlist.items.add(mediaFileEntity.copy(id = id).toModel())
        playlistAdapter.notifyItemChanged(position)
    }

    private fun launchMediaPicker(position: Int) {
        pendingPlaylistPosition = position
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        pickMediaLauncher.launch(intent)
    }

    private suspend fun addMediaToPlaylist(position: Int, uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // The provider does not offer persistable grants; the URI stays usable for this session.
        }

        val playlist = playlists.getOrNull(position) ?: return
        val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment ?: "Unknown"

        val mediaFileEntity = MediaFileEntity(
            playlistId = playlist.id,
            name = name,
            path = uri.toString(),
            isUrl = false
        )
        val id = playlistDao.insertMediaFile(mediaFileEntity)
        playlist.items.add(mediaFileEntity.copy(id = id).toModel())
        playlistAdapter.notifyItemChanged(position)
    }

    private fun playMedia(mediaFile: MediaFile) {
        val playlistIndex = playlists.indexOfFirst { it.items.contains(mediaFile) }
        if (playlistIndex != -1) {
            playPlaylist(playlistIndex, playlists[playlistIndex].items.indexOf(mediaFile))
        }
    }

    private fun playPlaylist(playlistPosition: Int, startIndex: Int) {
        val player = player ?: return
        try {
            val playlist = playlists[playlistPosition]
            if (playlist.items.isEmpty()) return
            val mediaItems = playlist.items.map { it.toMediaItem() }

            player.apply {
                setMediaItems(mediaItems, startIndex, 0L)
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                prepare()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing playlist: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun MediaFile.toMediaItem(): MediaItem {
        val uri = Uri.parse(path)
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(name).build())
            .build()
    }
}
