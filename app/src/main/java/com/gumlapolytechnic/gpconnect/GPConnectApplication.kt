package com.gumlapolytechnic.gpconnect

import android.app.Application
import com.gumlapolytechnic.gpconnect.data.mock.MockAdminAuthRepository
import com.gumlapolytechnic.gpconnect.data.mock.MockAuthRepository
import com.gumlapolytechnic.gpconnect.data.mock.MockEventPreviews
import com.gumlapolytechnic.gpconnect.data.mock.MockNoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.AdminAuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository

/**
 * Manual dependency container. Deliberately no DI framework — repositories are
 * constructed once and handed to ViewModels, keeping the swap from mock to
 * Firebase data sources (Phase 4) a single-line change.
 */
class AppContainer {
    val authRepository: AuthRepository = MockAuthRepository()
    val adminAuthRepository: AdminAuthRepository = MockAdminAuthRepository()
    val noticeRepository: NoticeRepository = MockNoticeRepository()

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
