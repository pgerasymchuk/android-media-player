package com.pgerasymchuk.mediaplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pgerasymchuk.mediaplayer.R
import com.pgerasymchuk.mediaplayer.data.model.MediaFile
import com.pgerasymchuk.mediaplayer.data.model.Playlist

class PlaylistAdapter(
    private val playlists: MutableList<Playlist>,
    private val onPlayClick: (MediaFile) -> Unit,
    private val onDeleteMediaClick: (Int, Int) -> Unit,
    private val onEditPlaylist: (Int) -> Unit,
    private val onDeletePlaylist: (Int) -> Unit,
    private val onToggleVisibility: (Int) -> Unit,
    private val onShufflePlaylist: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        val mediaRecyclerView: RecyclerView = itemView.findViewById(R.id.mediaRecyclerView)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val shuffleButton: Button = itemView.findViewById(R.id.shuffleButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.playlistName.text = playlist.name
        holder.mediaRecyclerView.visibility = if (playlist.isVisible) View.VISIBLE else View.GONE

        val mediaAdapter = MediaAdapter(
            playlist.items,
            onPlayClick,
            { mediaPosition -> holder.withPosition { onDeleteMediaClick(it, mediaPosition) } }
        )
        holder.mediaRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.mediaRecyclerView.adapter = mediaAdapter

        holder.playlistName.setOnClickListener { holder.withPosition(onToggleVisibility) }
        holder.editButton.setOnClickListener { holder.withPosition(onEditPlaylist) }
        holder.deleteButton.setOnClickListener { holder.withPosition(onDeletePlaylist) }
        holder.shuffleButton.setOnClickListener { holder.withPosition(onShufflePlaylist) }
    }

    // Positions captured at bind time go stale after inserts/removals,
    // so click handlers always ask the holder for its current position.
    private inline fun ViewHolder.withPosition(action: (Int) -> Unit) {
        val currentPosition = bindingAdapterPosition
        if (currentPosition != RecyclerView.NO_POSITION) action(currentPosition)
    }

    override fun getItemCount() = playlists.size
}