package com.gumlapolytechnic.gpconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.ui.admin.AdminApp
import com.gumlapolytechnic.gpconnect.ui.login.LoginNavHost
import com.gumlapolytechnic.gpconnect.ui.login.SessionState
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel
import com.gumlapolytechnic.gpconnect.ui.navigation.StudentApp
import com.gumlapolytechnic.gpconnect.ui.theme.GPConnectTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GPConnectApplication

        setContent {
            GPConnectTheme {
                // Root decision: the combined mock session state selects the
                // login graph, the student app, or the admin app. Because this
                // is a state switch (not navigation), no authenticated screen
                // can remain in a back stack after logout, and the student
                // shell can never appear during an admin session. Splash has
                // already dismissed; no artificial delays anywhere.
                val sessionViewModel: SessionViewModel = viewModel {
                    SessionViewModel(app.container.authRepository, app.container.adminAuthRepository)
                }
                val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

                Crossfade(targetState = sessionState, label = "session-state") { state ->
                    when (state) {
                        SessionState.LoggedOut -> LoginNavHost()
                        is SessionState.StudentActive -> StudentApp(
                            user = state.user,
                            onLogout = sessionViewModel::logoutStudent,
                        )
                        is SessionState.AdminActive -> AdminApp(
                            user = state.user,
                            onLogout = sessionViewModel::logoutAdmin,
                        )
                    }
                }
            }
        }
    }
}
