package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.isAdmin
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The possible root sessions. [Checking] is the startup state while a
 * persisted Firebase session is being restored — the login screen must never
 * appear during it, so a stored session never flashes LoginScreen.
 * AdminActive carries the resolved role on its [User]; the student shell can
 * never appear while an admin session is active, and vice versa.
 *
 * [StudentActive] is the *member* shell: it hosts STUDENT and TEACHER, which
 * share read-only content access and have no administrative portal.
 */
sealed interface SessionState {
    data object Checking : SessionState
    data object LoggedOut : SessionState
    data class StudentActive(val user: User) : SessionState
    data class AdminActive(val user: User) : SessionState {
        val role: UserRole get() = user.role
    }
}

/**
 * Root session state: derives the role-resolved Firebase session plus its
 * restoration progress into the shell decision consumed by the navigation
 * root. Logout flips straight to LoggedOut — restoration is already complete
 * by then, so no checking state delays the login screen.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val sessionState: StateFlow<SessionState> = combine(
        authRepository.session,
        authRepository.isResolvingSession,
    ) { user, resolving ->
        when {
            resolving -> SessionState.Checking
            user == null -> SessionState.LoggedOut
            // Positive test on the admin role set, not `!= STUDENT`: a TEACHER (or
            // any future non-admin role) must land in the member shell, never in
            // the admin portal with an unscoped module.
            user.role.isAdmin -> SessionState.AdminActive(user)
            else -> SessionState.StudentActive(user)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionState.Checking,
    )

    // --- Local-only canteen cart --------------------------------------------
    // Cart lives on the session because it must survive navigation between
    // canteen screens but must be cleared on logout. Never persisted, never
    // written to Firestore.

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState

    fun addToCart(menuItem: CanteenMenuItem) {
        if (!menuItem.isAvailable) return
        _cartState.value = _cartState.value.addItem(menuItem)
    }

    fun incrementCartItem(menuItemId: String) {
        _cartState.value = _cartState.value.increment(menuItemId)
    }

    fun decrementCartItem(menuItemId: String) {
        _cartState.value = _cartState.value.decrement(menuItemId)
    }

    fun removeCartItem(menuItemId: String) {
        _cartState.value = _cartState.value.remove(menuItemId)
    }

    fun clearCart() {
        _cartState.value = CartState()
    }

    fun logout() {
        // Cart is session-scoped: drop it on logout.
        _cartState.value = CartState()
        viewModelScope.launch { authRepository.logout() }
    }
}
