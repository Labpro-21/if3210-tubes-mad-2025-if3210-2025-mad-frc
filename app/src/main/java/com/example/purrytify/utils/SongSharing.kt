package com.example.purrytify.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.model.Song

fun shareServerSong(context: Context, song: Song) {
    val songIdToShare = song.serverId
    if (songIdToShare == null) {
        Log.w("ShareSong", "Attempted to share a song without a server ID.")
        // Tampilkan pesan ke pengguna jika perlu
        return
    }

    val deepLinkUrl = "purrytify://song/$songIdToShare"
    
    if (!song.audioPath.startsWith("http")) {
        Log.w("ShareSong", "Attempted to share a local song. Sharing is only for server songs.")
        // Atau tampilkan pesan ke pengguna
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