package com.example.purrytify.ui.screens

import androidx.compose.material.*
import androidx.compose.runtime.Composable

@Composable
fun NoInternetDialog(onDismiss: () -> Unit) {
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