package com.gumlapolytechnic.gpconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                // Root decision: Checking (session restoration) → branded
                // loader, then the login graph, the student app, or the
                // role-aware admin app. A state switch (not navigation) — no
                // authenticated screen can remain in a back stack after
                // logout, the shells are strictly isolated, and LoginScreen
                // never flashes for a persisted session. Splash has already
                // dismissed; no artificial delays anywhere.
                val sessionViewModel: SessionViewModel = viewModel {
                    SessionViewModel(app.container.authRepository)
                }
                val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

                Crossfade(targetState = sessionState, label = "session-state") { state ->
                    when (state) {
                        SessionState.Checking -> CheckingSessionScreen()
                        SessionState.LoggedOut -> LoginNavHost()
                        is SessionState.StudentActive -> StudentApp(
                            user = state.user,
                            onLogout = sessionViewModel::logout,
                        )
                        is SessionState.AdminActive -> AdminApp(
                            user = state.user,
                            onLogout = sessionViewModel::logout,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Branded startup state shown while a persisted Firebase session is being
 * resolved — the login screen must not appear behind it.
 */
@Composable
private fun CheckingSessionScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.gumla_polytechnic_logo),
                contentDescription = stringResource(R.string.cd_college_logo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
