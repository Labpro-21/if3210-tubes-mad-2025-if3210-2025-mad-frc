package com.example.purrytify.ui.screens

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import com.example.purrytify.ui.LockScreenOrientation
import android.content.pm.ActivityInfo

@Composable
fun NoInternetDialog(onDismiss: () -> Unit) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "No Internet Connection")
        },
        text = {
            Text(text = "Please check your connection and try again.")
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text(text = "OK")
            }
        }
    )
}