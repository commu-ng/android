package ng.commu.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private fun parseDate(dateString: String): Instant? {
    // Parse ISO8601 format (e.g., "2025-11-19T12:34:56.789Z")
    return try {
        Instant.parse(dateString)
    } catch (e: DateTimeParseException) {
        null
    }
}

fun formatRelativeTime(dateString: String): String {
    val instant = parseDate(dateString) ?: return dateString
    val now = Instant.now()
    val seconds = now.epochSecond - instant.epochSecond

    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}

fun formatFullDate(dateString: String): String {
    val instant = parseDate(dateString) ?: return dateString
    val formatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

object DateUtils {
    fun getRelativeTime(dateString: String): String {
        return formatRelativeTime(dateString)
    }

    fun formatTime(dateString: String): String {
        val instant = parseDate(dateString) ?: return dateString
        val formatter = DateTimeFormatter
            .ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
