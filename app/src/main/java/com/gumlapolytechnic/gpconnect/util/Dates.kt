package com.gumlapolytechnic.gpconnect.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Date formatting for notice timestamps. Uses java.text (not java.time) so it
 * runs on every device down to minSdk 24 without core library desugaring.
 */
object Dates {

    private val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    fun format(createdAt: Long): String = format.format(Date(createdAt))
}
