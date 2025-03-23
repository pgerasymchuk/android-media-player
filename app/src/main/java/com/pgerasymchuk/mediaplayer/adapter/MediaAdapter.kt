package com.pgerasymchuk.mediaplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pgerasymchuk.mediaplayer.R
import com.pgerasymchuk.mediaplayer.data.model.MediaFile

class MediaAdapter(
    private val mediaFiles: MutableList<MediaFile>,
    private val onPlayClick: (MediaFile) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val playButton: Button = itemView.findViewById(R.id.playButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaFile = mediaFiles[position]
        holder.fileName.text = mediaFile.name
        holder.playButton.setOnClickListener { onPlayClick(mediaFile) }
        holder.deleteButton.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) onDeleteClick(currentPosition)
        }
    }

    override fun getItemCount() = mediaFiles.size
}