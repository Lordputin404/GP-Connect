package com.gumlapolytechnic.gpconnect.ui.notices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Screen state for the notice list. isLoading/isEmpty/isError cover the
 * Loading / Empty / Error branches; a non-empty list is the Success branch.
 */
data class NoticesUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val search: String = "",
    val category: NoticeCategory? = null,
    val notices: List<Notice> = emptyList(),
    val readIds: Set<String> = emptySet(),
) {
    val isFiltered: Boolean get() = search.isNotBlank() || category != null
}

/**
 * Notice list state holder: search text and category filter combine into a
 * [NoticeQuery] that the repository evaluates — both filters always work
 * together. The mock repository answers instantly after its first fetch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoticesViewModel(private val noticeRepository: NoticeRepository) : ViewModel() {

    private val search = MutableStateFlow("")
    private val category = MutableStateFlow<NoticeCategory?>(null)
    private val refresh = MutableStateFlow(0)

    val uiState: StateFlow<NoticesUiState> = combine(
        combine(search, category, refresh) { queryText, queryCategory, _ ->
            NoticeQuery(search = queryText, category = queryCategory)
        }.flatMapLatest { query ->
            noticeRepository.observeNotices(query).map { notices -> query to notices }
        },
        noticeRepository.observeReadMarkerIds(),
    ) { (query, notices), readIds ->
        NoticesUiState(
            isLoading = false,
            isError = false,
            search = query.search,
            category = query.category,
            notices = notices,
            readIds = readIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoticesUiState(),
    )

    fun onSearchChange(value: String) {
        search.value = value
    }

    fun onCategoryChange(value: NoticeCategory?) {
        category.value = value
    }

    fun retry() {
        refresh.value += 1
    }
}
