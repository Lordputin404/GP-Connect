package com.gumlapolytechnic.gpconnect.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.mock.CampusEventPreview
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.EventPreviewShimmer
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.NoticePreviewCard
import com.gumlapolytechnic.gpconnect.ui.components.QuickAccessTile
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader
import com.gumlapolytechnic.gpconnect.ui.placeholder.CampusFeature

/**
 * Phase 2 Home dashboard: branded header with student welcome, important
 * (pinned) notices, upcoming events preview, quick access grid and recent
 * notices. Fully vertically scrollable; all content sections respond to the
 * loading state so the screen is never blank.
 */
@Composable
fun HomeScreen(
    user: User,
    onNoticeClick: (String) -> Unit,
    onViewAllNotices: () -> Unit,
    onFeatureClick: (CampusFeature) -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(app.container.noticeRepository, app.container.eventPreviews)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // --- Branded top area -------------------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.gumla_polytechnic_logo),
                contentDescription = stringResource(R.string.cd_college_logo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.college_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.course_semester_format, user.course, user.semester),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            HomeLoadingContent()
        } else if (state.isError) {
            ErrorState(message = stringResource(R.string.home_error_body))
        } else {
            HomeSections(state = state, onNoticeClick = onNoticeClick, onViewAllNotices = onViewAllNotices, onFeatureClick = onFeatureClick)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HomeLoadingContent() {
    SectionHeader(title = stringResource(R.string.section_important_notices))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) { NoticeCardShimmer() }
    }
    Spacer(modifier = Modifier.height(24.dp))
    SectionHeader(title = stringResource(R.string.section_upcoming_events))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) { EventPreviewShimmer() }
    }
    Spacer(modifier = Modifier.height(24.dp))
    SectionHeader(title = stringResource(R.string.section_recent_notices))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) { NoticeCardShimmer() }
    }
}

@Composable
private fun HomeSections(
    state: HomeUiState,
    onNoticeClick: (String) -> Unit,
    onViewAllNotices: () -> Unit,
    onFeatureClick: (CampusFeature) -> Unit,
) {
    // --- Important notices ------------------------------------------------
    SectionHeader(
        title = stringResource(R.string.section_important_notices),
        actionLabel = stringResource(R.string.action_view_all),
        onActionClick = onViewAllNotices,
    )
    if (state.importantNotices.isEmpty()) {
        Text(
            text = stringResource(R.string.home_no_pinned),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.importantNotices.forEach { notice ->
                NoticePreviewCard(
                    notice = notice,
                    isRead = notice.id in state.readIds,
                    onClick = { onNoticeClick(notice.id) },
                )
            }
        }
    }

    // --- Upcoming events (preview only, Phase 6) ---------------------------
    Spacer(modifier = Modifier.height(24.dp))
    SectionHeader(title = stringResource(R.string.section_upcoming_events))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.events.forEach { event -> EventPreviewCard(event = event) }
    }

    // --- Quick access ------------------------------------------------------
    Spacer(modifier = Modifier.height(24.dp))
    SectionHeader(title = stringResource(R.string.section_quick_access))
    QuickAccessGrid(onFeatureClick = onFeatureClick)

    // --- Recent notices ----------------------------------------------------
    Spacer(modifier = Modifier.height(24.dp))
    SectionHeader(
        title = stringResource(R.string.section_recent_notices),
        actionLabel = stringResource(R.string.action_view_all),
        onActionClick = onViewAllNotices,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.recentNotices.forEach { notice ->
            NoticePreviewCard(
                notice = notice,
                isRead = notice.id in state.readIds,
                onClick = { onNoticeClick(notice.id) },
            )
        }
    }
}

@Composable
private fun EventPreviewCard(event: CampusEventPreview) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = event.dayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = event.monthLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(onFeatureClick: (CampusFeature) -> Unit) {
    val rows = CampusFeature.entries.chunked(QUICK_ACCESS_COLUMNS)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowFeatures ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowFeatures.forEach { feature ->
                    QuickAccessTile(
                        label = stringResource(feature.titleRes),
                        icon = feature.icon,
                        onClick = { onFeatureClick(feature) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(QUICK_ACCESS_COLUMNS - rowFeatures.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val QUICK_ACCESS_COLUMNS = 3
