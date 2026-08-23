package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory

/** Label resource for each category (text always accompanies color). */
val NoticeCategory.labelRes: Int
    get() = when (this) {
        NoticeCategory.EXAM -> R.string.category_exam
        NoticeCategory.GENERAL -> R.string.category_general
        NoticeCategory.EVENT -> R.string.category_event
        NoticeCategory.HOLIDAY -> R.string.category_holiday
        NoticeCategory.LIBRARY -> R.string.category_library
        NoticeCategory.ASSIGNMENT -> R.string.category_assignment
    }

private val NoticeCategory.icon: ImageVector
    get() = when (this) {
        NoticeCategory.EXAM -> Icons.Outlined.Quiz
        NoticeCategory.GENERAL -> Icons.Outlined.Info
        NoticeCategory.EVENT -> Icons.Outlined.Celebration
        NoticeCategory.HOLIDAY -> Icons.Outlined.BeachAccess
        NoticeCategory.LIBRARY -> Icons.Outlined.MenuBook
        NoticeCategory.ASSIGNMENT -> Icons.Outlined.Assignment
    }

@Composable
private fun NoticeCategory.colorPair(): Pair<Color, Color> = when (this) {
    NoticeCategory.EXAM ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    NoticeCategory.GENERAL ->
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    NoticeCategory.EVENT ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    NoticeCategory.HOLIDAY ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    NoticeCategory.LIBRARY ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    NoticeCategory.ASSIGNMENT ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}

/**
 * Small category badge: icon + label on a tonal container. The label ensures
 * category information is never communicated through color alone.
 */
@Composable
fun CategoryBadge(category: NoticeCategory, modifier: Modifier = Modifier) {
    val (container, content) = category.colorPair()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(
                text = stringResource(category.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}
