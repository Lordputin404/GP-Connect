package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val uiState: StateFlow<CanteenUiState> = combine(
        canteenRepository.observeCategories(),
        canteenRepository.observeAvailableMenuItems(),
    ) { categories, menuItems ->
        CanteenUiState(
            isLoading = false,
            isError = false,
            categories = categories,
            menuItems = menuItems,
            selectedCategoryId = null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CanteenUiState(),
    )

    fun selectCategory(categoryId: String?) {
        viewModelScope.launch {
            val currentState = uiState.value
            when (categoryId) {
                null -> {
                    // Show all menu items when no category selected
                    // We'll need to re-fetch menu items for all categories
                    // For now, we'll just update the selectedCategoryId and rely on UI to filter
                    // TODO: Better approach would be to have separate flows for all vs category-specific
                }
                else -> {
                    // Show menu items for selected category
                    // We'll need to fetch category-specific items
                    // TODO: Implement category-specific menu fetching
                }
            }
        }
    }
}