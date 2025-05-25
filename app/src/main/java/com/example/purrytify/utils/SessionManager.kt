package com.example.purrytify.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "key_user_id"
    }

    fun saveSession(userId: Int) {
        prefs.edit() {
            putInt(KEY_USER_ID, userId)
        }
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, 1)
    }

    fun clearSession() {
        prefs.edit() { clear() }
    }
}