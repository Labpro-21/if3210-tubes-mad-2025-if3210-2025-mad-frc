package com.example.purrytify.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.purrytify.R
import com.example.purrytify.model.Song
import androidx.core.net.toUri

class SongRecyclerViewAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song) -> Unit,
    private val onToggleLike: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerViewAdapter.SongViewHolder>() {

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.song_title)
        val artist = view.findViewById<TextView>(R.id.song_artist)
        val artwork = view.findViewById<ImageView>(R.id.song_artwork)
        val likeButton = view.findViewById<ImageView>(R.id.like_button)

        fun bind(song: Song) {
            val displayTitle = if (song.title.length > 23) {
                song.title.take(20) + "..."
            } else {
                song.title
            }
            title.text = displayTitle
            val displayArtist = if (song.artist.length > 23) {
                song.artist.take(20) + "..."
            } else {
                song.artist
            }
            artist.text = displayArtist


            likeButton.setImageResource(
                if (song.liked) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
            )

            // Load artwork image from URI using Glide
            val artworkUri = song.artworkPath?.toUri()
            if (artworkUri != null) {
                Glide.with(itemView.context)
                    .load(artworkUri)
                    .placeholder(R.drawable.placeholder_music) // default placeholder
                    .error(R.drawable.placeholder_music)       // error fallback
                    .into(artwork)
            } else {
                artwork.setImageResource(R.drawable.placeholder_music)
            }

            itemView.setOnClickListener { onSongClick(song) }
            likeButton.setOnClickListener { onToggleLike(song) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }
}
