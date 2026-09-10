package com.gumlapolytechnic.gpconnect.ui.calendar

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import com.gumlapolytechnic.gpconnect.data.model.EventAttachmentType
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.EventTypeBadge
import com.gumlapolytechnic.gpconnect.ui.components.EventStatusChip
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.util.AttachmentOpener
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Calendar event detail: full title, type/status chips, date or date range,
 * all-day note, description, and this event's attachments. Follows the
 * NoticeDetailScreen layout. Tapping an attachment downloads it into the
 * private cache and opens it via ACTION_VIEW through the FileProvider — a
 * missing viewer app shows a message instead of a crash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventDetailScreen(eventId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val context = LocalContext.current
    val viewModel: CalendarEventDetailViewModel = viewModel(key = eventId) {
        CalendarEventDetailViewModel(app.container.calendarRepository, eventId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Cache dir is provided by the opener; downloads mirror the Storage path.
    val downloadFailedText = stringResource(R.string.calendar_attachment_open_failed)
    val noViewerText = stringResource(R.string.calendar_attachment_no_app)

    Scaffold(
        topBar = {
            TopAppBar(
                // Same inset rationale as NoticeDetailScreen: the outer student
                // Scaffold already consumes the status-bar inset.
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.calendar_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(3) { NoticeCardShimmer() }
                }
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ErrorState(message = stringResource(R.string.calendar_error_body))
                }
            }
            state.event == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.calendar_event_not_found_title),
                        message = stringResource(R.string.calendar_event_not_found_body),
                    )
                }
            }
            else -> {
                EventDetailContent(
                    event = state.event!!,
                    openingAttachment = state.openingAttachment,
                    onOpenAttachment = { attachment ->
                        viewModel.openAttachment(
                            attachment = attachment,
                            cacheDirectory = AttachmentOpener.cacheDirectory(context),
                            onDownloaded = { file ->
                                // The screen owns the window context, so the
                                // ACTION_VIEW happens here; a missing viewer
                                // is reported back to the ViewModel's message
                                // flow (surfaced as a toast below).
                                if (AttachmentOpener.open(context, attachment, file) ==
                                    AttachmentOpener.OpenAttachmentResult.NoViewer
                                ) {
                                    viewModel.onNoViewerApp()
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }

    // Transient, user-friendly failure messages — never a crash. (The project
    // has no snackbar host; a toast is the smallest footprint that satisfies
    // "clear message instead of crashing".)
    val message = state.openMessage
    if (message != null) {
        LaunchedEffect(message) {
            Toast.makeText(
                context,
                when (message) {
                    AttachmentOpenMessage.DOWNLOAD_FAILED -> downloadFailedText
                    AttachmentOpenMessage.NO_VIEWER -> noViewerText
                },
                Toast.LENGTH_LONG,
            ).show()
            viewModel.consumeOpenMessage()
        }
    }
}

@Composable
private fun EventDetailContent(
    event: CalendarEvent,
    openingAttachment: EventAttachment?,
    onOpenAttachment: (EventAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventTypeBadge(type = event.type)
            Spacer(modifier = Modifier.width(8.dp))
            EventStatusChip(status = event.status)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Dates.formatRange(event.startDate, event.endDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (event.isAllDay) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.calendar_all_day),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (event.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // --- Related attachments (only this event's; hidden when empty) -----
        if (event.attachments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.calendar_attachments_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            event.attachments.forEach { attachment ->
                AttachmentTile(
                    attachment = attachment,
                    isOpening = openingAttachment == attachment,
                    onOpen = { onOpenAttachment(attachment) },
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * One attachment row: type icon, file name, "TYPE • size" metadata line and
 * the Open action (a spinner while its download runs). Doc-family files get
 * the attach icon, PDFs the document icon, images the picture icon.
 */
@Composable
private fun AttachmentTile(
    attachment: EventAttachment,
    isOpening: Boolean,
    onOpen: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (attachment.type) {
                    EventAttachmentType.PDF -> Icons.Outlined.PictureAsPdf
                    EventAttachmentType.DOC, EventAttachmentType.DOCX -> Icons.Outlined.AttachFile
                    EventAttachmentType.JPG, EventAttachmentType.JPEG,
                    EventAttachmentType.PNG, EventAttachmentType.WEBP -> Icons.Outlined.Image
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = attachmentMetaLabel(attachment),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOpening) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.calendar_attachment_open))
                }
            }
        }
    }
}

/** "PDF • 1.2 MB" — type label and human-readable size. */
@Composable
private fun attachmentMetaLabel(attachment: EventAttachment): String {
    val sizeText = when {
        attachment.size >= 1_048_576 ->
            stringResource(R.string.calendar_attachment_size_mb, attachment.size / 1_048_576f)
        attachment.size >= 1_024 ->
            stringResource(R.string.calendar_attachment_size_kb, attachment.size / 1_024)
        else -> "${attachment.size} B"
    }
    return stringResource(
        R.string.admin_calendar_attachment_meta,
        attachment.type.name,
        sizeText,
    )
}
