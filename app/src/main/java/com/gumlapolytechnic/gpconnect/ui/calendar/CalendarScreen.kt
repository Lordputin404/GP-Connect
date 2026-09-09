package com.gumlapolytechnic.gpconnect.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.ui.components.CalendarEventCard
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.labelRes

/**
 * College Calendar for members (Student/Teacher/HOD): published events with a
 * type filter row, upcoming events first. The same screen backs the calendar
 * tab for every member role — read authority is enforced by Firestore rules.
 */
@Composable
fun CalendarScreen(onEventClick: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CalendarViewModel =
        viewModel { CalendarViewModel(app.container.calendarRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.calendar_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            item {
                CategoryChip(
                    label = stringResource(R.string.filter_all),
                    selected = state.type == null,
                    onClick = { viewModel.onTypeChange(null) },
                )
            }
            items(CalendarEventType.entries.toList()) { type ->
                CategoryChip(
                    label = stringResource(type.labelRes),
                    selected = state.type == type,
                    onClick = {
                        viewModel.onTypeChange(if (state.type == type) null else type)
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { NoticeCardShimmer() }
                }
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ErrorState(
                        message = stringResource(R.string.calendar_error_body),
                        onRetry = viewModel::retry,
                    )
                }
            }
            state.events.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(
                            if (state.type == null) {
                                R.string.calendar_empty_title
                            } else {
                                R.string.calendar_empty_filtered_title
                            },
                        ),
                        message = stringResource(R.string.calendar_empty_body),
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        CalendarEventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                        )
                    }
                }
            }
        }
    }
}
