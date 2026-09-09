package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventStatus
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.util.Dates

/** Label resource for each calendar event type (text always accompanies color). */
val CalendarEventType.labelRes: Int
    get() = when (this) {
        CalendarEventType.EXAM -> R.string.calendar_type_exam
        CalendarEventType.HOLIDAY -> R.string.calendar_type_holiday
        CalendarEventType.ACTIVITY -> R.string.calendar_type_activity
        CalendarEventType.CO_CURRICULAR -> R.string.calendar_type_co_curricular
    }

/** Label resource for a calendar event status. */
val CalendarEventStatus.labelRes: Int
    get() = when (this) {
        CalendarEventStatus.CONFIRMED -> R.string.calendar_status_confirmed
        CalendarEventStatus.TENTATIVE -> R.string.calendar_status_tentative
    }

/**
 * Tonal colors for a calendar event type. The label always accompanies the
 * color (never color alone), mirroring [CategoryBadge].
 */
@Composable
private fun CalendarEventType.colorPair(): Pair<Color, Color> = when (this) {
    CalendarEventType.EXAM ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    CalendarEventType.HOLIDAY ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    CalendarEventType.ACTIVITY ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    CalendarEventType.CO_CURRICULAR ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}

/**
 * Calendar event card: type badge + status chip, title, date or date range.
 * Shared by the member College Calendar list, the Home upcoming preview and
 * the admin management list.
 */
@Composable
fun CalendarEventCard(
    event: CalendarEvent,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = onClick != null,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventTypeBadge(type = event.type)
                EventStatusChip(status = event.status)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Dates.formatRange(event.startDate, event.endDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Type badge for a calendar event: label on the type's tonal container. */
@Composable
fun EventTypeBadge(type: CalendarEventType, modifier: Modifier = Modifier) {
    val (container, content) = type.colorPair()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
    ) {
        Text(
            text = stringResource(type.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * Status chip for a calendar event. TENTATIVE is emphasized with the tertiary
 * tonal palette; CONFIRMED stays quiet so it reads as the default state.
 */
@Composable
fun EventStatusChip(status: CalendarEventStatus, modifier: Modifier = Modifier) {
    val container: Color
    val content: Color
    when (status) {
        CalendarEventStatus.CONFIRMED -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        CalendarEventStatus.TENTATIVE -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
        }
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
    ) {
        Text(
            text = stringResource(status.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
