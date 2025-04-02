package com.pgerasymchuk.mediaplayer.activity

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pgerasymchuk.mediaplayer.databinding.ActivityRadioBinding
import com.pgerasymchuk.mediaplayer.service.MediaPlaybackService

class RadioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRadioBinding
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var player: MediaController? = null

    private val errorListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(
                this@RadioActivity,
                "Error playing radio: ${error.errorCodeName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRadioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.radioPlayerView.useController = true

        binding.playRadioButton.setOnClickListener {
            val url = binding.radioUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                playRadio(url)
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            if (future.isCancelled) return@addListener
            player = future.get().also { it.addListener(errorListener) }
            binding.radioPlayerView.player = player
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        binding.radioPlayerView.player = null
        player?.removeListener(errorListener)
        player = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun playRadio(url: String) {
        val player = player ?: return
        try {
            val uri = Uri.parse(url)
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(uri.host ?: url).build())
                .build()
            player.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing radio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
