package com.example.purrytify.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.model.Song

fun shareServerSong(context: Context, song: Song) {
    val songIdToShare = song.serverId
    if (songIdToShare == null) {
        Log.w("ShareSong", "Attempted to share a song without a server ID.")

        return
    }

    val deepLinkUrl = "purrytify:
    
    if (!song.audioPath.startsWith("http")) {
        Log.w("ShareSong", "Attempted to share a local song. Sharing is only for server songs.")

        return
    }

    val songId = song.id
    val shareText = "$deepLinkUrl"

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, deepLinkUrl)
        type = "text/link"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Song via")
    context.startActivity(shareIntent)
}