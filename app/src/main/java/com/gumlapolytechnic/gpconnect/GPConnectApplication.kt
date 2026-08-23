package com.gumlapolytechnic.gpconnect

import android.app.Application
import com.gumlapolytechnic.gpconnect.data.mock.MockAuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository

/**
 * Manual dependency container. Deliberately no DI framework — repositories are
 * constructed once and handed to ViewModels, keeping the swap from mock to
 * Firebase data sources (Phase 4) a single-line change.
 */
class AppContainer {
    val authRepository: AuthRepository = MockAuthRepository()
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
