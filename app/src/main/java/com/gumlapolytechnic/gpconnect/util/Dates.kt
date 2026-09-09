package com.gumlapolytechnic.gpconnect.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date formatting for notice and calendar timestamps. Uses java.text (not
 * java.time) so it runs on every device down to minSdk 24 without core
 * library desugaring.
 */
object Dates {

    private val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    private val shortMonthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val rangeFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    fun format(createdAt: Long): String = format.format(Date(createdAt))

    /** Day-of-month label ("18") for the compact event date block. */
    fun dayLabel(timestampMs: Long): String = dayFormat.format(Date(timestampMs))

    /** Short month label ("Oct") for the compact event date block. */
    fun shortMonthLabel(timestampMs: Long): String = shortMonthFormat.format(Date(timestampMs))

    /**
     * "d MMM yyyy" for a whole-day range when both days differ, or a single
     * date otherwise. All-day events carry no time of day, so only the day
     * matters. The single-date format matches [format].
     */
    fun formatRange(startMs: Long, endMs: Long): String {
        if (isSameDay(startMs, endMs)) return format.format(Date(startMs))
        return "${rangeFormat.format(Date(startMs))} – ${rangeFormat.format(Date(endMs))}"
    }

    /** Epoch-ms of the local start of day containing [timestampMs]. */
    fun startOfDay(timestampMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun isSameDay(a: Long, b: Long): Boolean =
        startOfDay(a) == startOfDay(b)
}
