package com.gallery.app.utils

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    fun formatFileSize(context: Context, bytes: Long?): String {
        if (bytes == null) return "Unknown"
        return Formatter.formatFileSize(context, bytes)
    }

    fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0) return "0:00"
        val totalSeconds = durationMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null) return "Unknown"
        val sdf = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun formatResolution(width: Int?, height: Int?): String {
        return if (width != null && height != null) {
            "${width} × ${height}"
        } else {
            "Unknown"
        }
    }

    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
