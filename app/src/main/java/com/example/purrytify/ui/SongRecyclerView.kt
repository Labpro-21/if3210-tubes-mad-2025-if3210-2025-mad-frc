package com.example.purrytify.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.purrytify.model.Song


@Composable
fun SongRecyclerView(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onToggleLike: (Song) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                adapter = SongRecyclerViewAdapter(songs, onSongClick, onToggleLike)
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as? SongRecyclerViewAdapter)?.updateSongs(songs)
        },
        modifier = Modifier.fillMaxWidth()
    )
}
