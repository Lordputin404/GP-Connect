package com.gumlapolytechnic.gpconnect

import android.app.Application
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseAuthRepository
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseCanteenRepository
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseNoticeRepository
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseOrderRepository
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseSignupRequestRepository
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseUserRepository
import com.gumlapolytechnic.gpconnect.data.mock.MockEventPreviews
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.OrderRepository
import com.gumlapolytechnic.gpconnect.data.repository.SignupRequestRepository
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository

/**
 * Manual dependency container. Since Phase 4B the production repositories are
 * Firebase-backed (Authentication + Firestore); the retained mock notice
 * repository exists only as a migration reference and is not wired here.
 */
class AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val noticeRepository: NoticeRepository = FirebaseNoticeRepository()
    val userRepository: UserRepository = FirebaseUserRepository()
    val signupRequestRepository: SignupRequestRepository = FirebaseSignupRequestRepository()
    val orderRepository: OrderRepository = FirebaseOrderRepository()
    val canteenRepository: CanteenRepository = FirebaseCanteenRepository()

    /** Phase 6 replaces this preview list with the real Events module. */
    val eventPreviews = MockEventPreviews.upcoming
}

/** Application entry point. Hosts the dependency container. */
class GPConnectApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
