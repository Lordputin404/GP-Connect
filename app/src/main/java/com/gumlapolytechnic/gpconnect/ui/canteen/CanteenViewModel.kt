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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Canteen catalog screens.
 * Handles loading categories and menu items from the repository.
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

    private val categoriesFlow = canteenRepository.observeCategories()
        .onEach { }
        .catch { e ->
            // Error is handled in the combined state
        }

    private val menuItemsFlow = _selectedCategoryId.flatMapLatest { categoryId: String? ->
        when (categoryId) {
            null -> canteenRepository.observeAvailableMenuItems()
            else -> canteenRepository.observeAvailableMenuItemsByCategory(categoryId)
        }
    }.onEach { }
    .catch { e ->
        // Error is handled in the combined state
    }

    val uiState: StateFlow<CanteenUiState> = combine(
        categoriesFlow,
        menuItemsFlow,
    ) { categories: List<CanteenCategory>, menuItems: List<CanteenMenuItem> ->
        CanteenUiState(
            isLoading = false,
            isError = categories.isEmpty() && menuItems.isEmpty(),
            categories = categories,
            menuItems = menuItems,
            selectedCategoryId = _selectedCategoryId.value,
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