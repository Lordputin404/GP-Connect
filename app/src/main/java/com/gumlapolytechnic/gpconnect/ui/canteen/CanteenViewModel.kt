package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Canteen catalog screens.
 * Handles loading categories and menu items from the repository.
 *
 * Errors from the repository/listener are surfaced through [CanteenUiState.isError]
 * so the UI can leave the loading/skeleton state and show a real error message.
 */
class CanteenViewModel(
    private val canteenRepository: CanteenRepository,
) : ViewModel() {

    data class CanteenUiState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val categories: List<CanteenCategory> = emptyList(),
        val menuItems: List<CanteenMenuItem> = emptyList(),
        val selectedCategoryId: String? = null,
    )

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _hasError = MutableStateFlow(false)

    private val categoriesFlow = canteenRepository.observeCategories()
        .onEach { _hasError.value = false }
        .catch { _ -> _hasError.value = true }

    private val menuItemsFlow = _selectedCategoryId.flatMapLatest { categoryId: String? ->
        when (categoryId) {
            null -> canteenRepository.observeAvailableMenuItems()
            else -> canteenRepository.observeAvailableMenuItemsByCategory(categoryId)
        }
    }
        .onEach { _hasError.value = false }
        .catch { _ -> _hasError.value = true }

    val uiState: StateFlow<CanteenUiState> = combine(
        categoriesFlow,
        menuItemsFlow,
        _selectedCategoryId,
        _hasError,
    ) { categories: List<CanteenCategory>,
        menuItems: List<CanteenMenuItem>,
        selectedId: String?,
        hasError: Boolean ->
        CanteenUiState(
            isLoading = false,
            isError = hasError && categories.isEmpty() && menuItems.isEmpty(),
            categories = categories,
            menuItems = menuItems,
            selectedCategoryId = selectedId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CanteenUiState(),
    )

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }
}
