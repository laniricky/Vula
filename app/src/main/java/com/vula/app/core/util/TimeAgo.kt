package com.vula.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeAgo {
    fun format(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000L             -> "just now"
            diff < 3_600_000L          -> "${diff / 60_000}m ago"
            diff < 86_400_000L         -> "${diff / 3_600_000}h ago"
            diff < 7 * 86_400_000L     -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
