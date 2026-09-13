package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.repository.CanteenCategoryDraft
import com.gumlapolytechnic.gpconnect.data.repository.CanteenMenuItemDraft
import com.gumlapolytechnic.gpconnect.data.repository.CanteenQuery
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCanteenUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val categories: List<CanteenCategory> = emptyList(),
    val items: List<CanteenMenuItem> = emptyList(),
    /** itemId → category name, for the management cards. */
    val categoryNames: Map<String, String> = emptyMap(),
    val busyIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
)

/**
 * Canteen Management state for the CANTEEN_ADMIN (and SUPER_ADMIN): the
 * full category and item catalog (including disabled/unavailable), plus
 * the category CRUD/toggle and item delete/availability mutations. Writes
 * go through the repository and return [Result] — the Firestore rules
 * reject any other caller, which this UI surfaces as an error banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminCanteenViewModel(
    private val canteenRepository: CanteenRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)

    private val catalog = refresh.flatMapLatest {
        combine(
            canteenRepository.observeCategories(CanteenQuery(membersMenuOnly = false)),
            canteenRepository.observeMenuItems(CanteenQuery(membersMenuOnly = false)),
        ) { categories, items -> categories to items }
    }

    val uiState: StateFlow<AdminCanteenUiState> =
        combine(catalog, busyIds, actionFailed) { (categoriesResult, itemsResult), busy, failed ->
            val categories = categoriesResult.getOrElse { emptyList() }
            val items = itemsResult.getOrElse { emptyList() }
            AdminCanteenUiState(
                isLoading = false,
                isError = categoriesResult.isFailure || itemsResult.isFailure,
                categories = categories,
                items = items,
                categoryNames = categories.associate { it.id to it.name },
                busyIds = busy,
                actionFailed = failed,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminCanteenUiState(),
        )

    // ---- categories -----------------------------------------------------

    fun saveCategory(category: CanteenCategory?, name: String, displayOrder: Int) = act(category?.id ?: "") {
        if (category == null) {
            canteenRepository.createCategory(
                CanteenCategoryDraft(name = name, displayOrder = displayOrder, enabled = true),
            )
        } else {
            canteenRepository.updateCategory(
                category.copy(name = name, displayOrder = displayOrder),
            )
        }
    }

    fun setCategoryEnabled(category: CanteenCategory, enabled: Boolean) = act(category.id) {
        canteenRepository.setCategoryEnabled(category.id, enabled)
    }

    fun deleteCategory(category: CanteenCategory) = act(category.id) {
        canteenRepository.deleteCategory(category.id)
    }

    // ---- menu items ------------------------------------------------------

    fun setItemAvailable(item: CanteenMenuItem, available: Boolean) = act(item.id) {
        canteenRepository.setItemAvailable(item.id, available)
    }

    fun deleteMenuItem(item: CanteenMenuItem) = act(item.id) {
        canteenRepository.deleteMenuItem(item.id)
    }

    fun dismissActionError() {
        actionFailed.value = false
    }

    fun retry() {
        refresh.value += 1
    }

    private fun act(id: String, action: suspend () -> Result<Unit>) {
        if (id in busyIds.value) return
        busyIds.update { it + id }
        actionFailed.value = false
        viewModelScope.launch {
            val result = action()
            busyIds.update { it - id }
            if (result.isFailure) actionFailed.value = true
        }
    }
}

data class AdminCanteenItemFormUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val name: String = "",
    val categoryId: String? = null,
    val priceText: String = "",
    val description: String = "",
    val isAvailable: Boolean = true,
    val displayOrderText: String = "",
    /** Enabled categories to choose from (live, from Firestore). */
    val categories: List<CanteenCategory> = emptyList(),
    val nameError: Boolean = false,
    val categoryError: Boolean = false,
    val priceError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Menu item create/edit form state (the AdminBookFormViewModel pattern).
 * The category is chosen from the live enabled-category chips — never free
 * text; the save stays blocked until a category is selected. Price is a
 * whole-rupee count. Create mode files a [CanteenMenuItemDraft]; edit
 * mode preloads by ID, preserves id/createdAt, and updates in place.
 */
class AdminCanteenItemFormViewModel(
    private val canteenRepository: CanteenRepository,
    private val editItemId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminCanteenItemFormUiState(
            isLoading = editItemId != null,
            isEditMode = editItemId != null,
        ),
    )
    val uiState: StateFlow<AdminCanteenItemFormUiState> = _uiState.asStateFlow()

    init {
        // Live category chips: keep the choices current even while the form
        // is open, and reflect a disabled/removed category immediately.
        viewModelScope.launch {
            canteenRepository.observeCategories(CanteenQuery(membersMenuOnly = false))
                .collect { result ->
                    val categories = result.getOrNull().orEmpty()
                    _uiState.update { it.copy(categories = categories.filter { category -> category.enabled }) }
                }
        }
        val id = editItemId
        if (id != null) {
            viewModelScope.launch {
                val item = canteenRepository.getMenuItem(id)
                _uiState.update { state ->
                    if (item == null) {
                        state.copy(isLoading = false, notFound = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            name = item.name,
                            categoryId = item.categoryId,
                            priceText = if (item.price > 0) item.price.toString() else "",
                            description = item.description,
                            isAvailable = item.isAvailable,
                            displayOrderText = if (item.displayOrder != 0) item.displayOrder.toString() else "",
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = false) }
    }

    fun onCategoryChange(categoryId: String) {
        _uiState.update { it.copy(categoryId = categoryId, categoryError = false) }
    }

    fun onPriceChange(value: String) {
        _uiState.update { it.copy(priceText = value.filter { digit -> digit.isDigit() }, priceError = false) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onAvailableChange(value: Boolean) {
        _uiState.update { it.copy(isAvailable = value) }
    }

    fun onDisplayOrderChange(value: String) {
        _uiState.update { it.copy(displayOrderText = value.filter { digit -> digit.isDigit() }) }
    }

    fun save() {
        val state = _uiState.value
        val price = state.priceText.toIntOrNull() ?: 0
        val nameError = state.name.isBlank()
        val categoryError = state.categoryId == null
        val priceError = price <= 0
        if (nameError || categoryError || priceError) {
            _uiState.update {
                it.copy(nameError = nameError, categoryError = categoryError, priceError = priceError)
            }
            return
        }

        val categoryId = state.categoryId!!
        val displayOrder = state.displayOrderText.toIntOrNull() ?: 0
        _uiState.update {
            it.copy(isSaving = true, nameError = false, categoryError = false, priceError = false, saveError = false)
        }
        viewModelScope.launch {
            val succeeded = if (editItemId == null) {
                canteenRepository.createMenuItem(
                    CanteenMenuItemDraft(
                        name = state.name.trim(),
                        categoryId = categoryId,
                        price = price,
                        description = state.description.trim(),
                        isAvailable = state.isAvailable,
                        displayOrder = displayOrder,
                    ),
                ).isSuccess
            } else {
                val existing = canteenRepository.getMenuItem(editItemId)
                if (existing != null) {
                    canteenRepository.updateMenuItem(
                        existing.copy(
                            name = state.name.trim(),
                            categoryId = categoryId,
                            price = price,
                            description = state.description.trim(),
                            isAvailable = state.isAvailable,
                            displayOrder = displayOrder,
                        ),
                    ).isSuccess
                } else {
                    false
                }
            }
            _uiState.update {
                if (succeeded) {
                    it.copy(isSaving = false, saved = true)
                } else {
                    it.copy(isSaving = false, saveError = true)
                }
            }
        }
    }
}
