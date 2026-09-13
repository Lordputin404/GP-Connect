package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.model.CartLine
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

/** Student canteen menu state — the college-wide catalog, one shared menu. */
data class CanteenUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** Selected category id; null = All. */
    val selectedCategoryId: String? = null,
    val categories: List<CanteenCategory> = emptyList(),
    val items: List<CanteenMenuItem> = emptyList(),
) {
    val isFiltered: Boolean get() = selectedCategoryId != null
}

/**
 * College-wide canteen menu: enabled categories and available items for
 * every member — no department scoping exists in the canteen at all. The
 * cart badge lives on the screen itself via the shared [CartViewModel];
 * this state holder is purely the catalog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CanteenViewModel(
    canteenRepository: CanteenRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)
    private val selectedCategoryId = MutableStateFlow<String?>(null)

    private val categories = refresh.flatMapLatest { _ ->
        canteenRepository.observeCategories(CanteenQuery(membersMenuOnly = true))
    }
    private val items = refresh.flatMapLatest { _ ->
        canteenRepository.observeMenuItems(CanteenQuery(membersMenuOnly = true))
    }

    val uiState: StateFlow<CanteenUiState> =
        combine(
            categories,
            items,
            selectedCategoryId,
        ) { categoriesResult, itemsResult, selected ->
            val categoriesList = categoriesResult.getOrElse { emptyList() }
            val itemsList = itemsResult.getOrElse { emptyList() }
            // Only items whose category is still enabled are shown: the
            // rules guarantee each member read is enabled/available already,
            // but the two listeners can land a frame apart.
            val visibleCategories = categoriesList.filter { it.enabled }
            val visibleCategoryIds = visibleCategories.map { it.id }.toSet()
            val visibleItems = itemsList.filter { item ->
                item.isAvailable && item.categoryId in visibleCategoryIds
            }
            CanteenUiState(
                isLoading = false,
                isError = categoriesResult.isFailure || itemsResult.isFailure,
                selectedCategoryId = selected,
                categories = visibleCategories,
                items = if (selected == null) {
                    visibleItems
                } else {
                    visibleItems.filter { it.categoryId == selected }
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CanteenUiState(),
        )

    fun onCategoryChange(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun retry() {
        refresh.value += 1
    }
}

/** Canteen menu item detail state, resolved from the menu snapshot by id. */
data class CanteenItemDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isError: Boolean = false,
    val category: CanteenCategory? = null,
    val item: CanteenMenuItem? = null,
)

/**
 * Streams the member menu (enabled categories + available items) and
 * selects one item by id, resolving its category name. An id that is not
 * available (or whose category is disabled) shows "not found" — members
 * can only ever see the real menu.
 */
class CanteenItemDetailViewModel(
    canteenRepository: CanteenRepository,
    private val itemId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanteenItemDetailUiState())
    val uiState: StateFlow<CanteenItemDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = canteenRepository.observeCategories(CanteenQuery(membersMenuOnly = true))
            val items = canteenRepository.observeMenuItems(CanteenQuery(membersMenuOnly = true))
            combine(categories, items) { categoriesResult, itemsResult ->
                val enabledCategories = categoriesResult.getOrElse { emptyList() }.filter { it.enabled }
                val item = itemsResult.getOrNull()
                    ?.firstOrNull { it.id == itemId && it.isAvailable }
                    ?.takeIf { it.categoryId in enabledCategories.map { category -> category.id } }
                val category = enabledCategories.firstOrNull { it.id == item?.categoryId }
                CanteenItemDetailUiState(
                    isLoading = false,
                    isError = categoriesResult.isFailure || itemsResult.isFailure,
                    notFound = item == null,
                    category = category,
                    item = item,
                )
            }.collect { _uiState.value = it }
        }
    }
}

// ---- shared session cart ---------------------------------------------------

/** Immutable snapshot of the in-memory cart. */
data class CartUiState(
    /** One entry per distinct item; never two lines of the same product. */
    val lines: List<CartLine> = emptyList(),
) {
    /** Total item count across all products — the cart badge value. */
    val totalQuantity: Int get() = lines.sumOf { it.quantity }

    /** Sum of price × quantity across all lines. */
    val totalPrice: Int get() = lines.sumOf { it.lineTotal }

    /** itemId → quantity, consumed by the menu screen badges. */
    val quantities: Map<String, Int> get() = lines.associate { it.item.id to it.quantity }
}

/**
 * The session cart: local in-memory state only — never written to
 * Firestore, never persisted. It is shared by the menu, item detail and
 * cart screens (scoped to the student nav graph via the activity's
 * ViewModelStore), so it clears naturally when the app/session is
 * recreated.
 *
 * The same product can never appear twice: adding an existing item just
 * increments its quantity. Order placement is out of scope — the Order
 * button does nothing yet.
 */
class CartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    /** Adds the item with quantity 1 — or increments it if already present. */
    fun addItem(item: CanteenMenuItem) {
        if (!item.isAvailable) return
        _uiState.update { state ->
            val existing = state.lines.firstOrNull { it.item.id == item.id }
            if (existing != null) {
                state.copy(
                    lines = state.lines.map {
                        if (it.item.id == item.id) it.copy(quantity = it.quantity + 1) else it
                    },
                )
            } else {
                state.copy(lines = state.lines + CartLine(item = item, quantity = 1))
            }
        }
    }

    /** +1 on a cart line; no-op when the item is gone or unavailable. */
    fun increment(itemId: String) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map {
                    if (it.item.id == itemId && it.item.isAvailable) {
                        it.copy(quantity = it.quantity + 1)
                    } else {
                        it
                    }
                },
            )
        }
    }

    /**
     * −1 on a cart line; when the quantity is 1 the line is removed
     * completely, as specified.
     */
    fun decrement(itemId: String) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.mapNotNull {
                    when {
                        it.item.id != itemId -> it
                        it.quantity <= 1 -> null
                        else -> it.copy(quantity = it.quantity - 1)
                    }
                },
            )
        }
    }

    fun clear() {
        _uiState.value = CartUiState()
    }
}
