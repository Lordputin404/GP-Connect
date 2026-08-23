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
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.login.LoginNavHost
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
                // Root decision: the mock auth state swaps the login graph for
                // the student app at the composition root. Because this is a
                // state switch (not navigation), login is never left in the
                // authenticated back stack and back from the app follows
                // normal Android behavior. Splash has already dismissed; no
                // artificial delays anywhere in this flow.
                val sessionViewModel: SessionViewModel =
                    viewModel { SessionViewModel(app.container.authRepository) }
                val user by sessionViewModel.authState.collectAsStateWithLifecycle()

                Crossfade(targetState = user, label = "auth-state") { currentUser ->
                    when (currentUser) {
                        null -> LoginNavHost()
                        is User -> StudentApp(
                            user = currentUser,
                            onLogout = sessionViewModel::logout,
                        )
                    }
                }
            }
        }
    }
}
