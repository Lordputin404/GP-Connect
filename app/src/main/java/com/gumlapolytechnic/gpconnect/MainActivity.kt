package com.gumlapolytechnic.gpconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

                // Notification permission for FCM on Android 13+. Asked once
                // per *signed-in session* (not per app start): the launcher is
                // armed before the shell appears and fires a single
                // LaunchedEffect when an authenticated shell is showing and
                // the permission is still undetermined. A denial is never
                // re-asked within the session — the system honors
                // "don't ask again" on its own for later sessions.
                NotificationPermissionRequester(sessionState)
            }
        }
    }
}

/**
 * Fires the single POST_NOTIFICATIONS request for this session. The
 * `hasAsked` guard is session-local; `rememberSaveable` would defeat the
 * per-session semantics, and re-composition can never retrigger because
 * the effect runs only while `shouldAsk` transitions true.
 */
@Composable
private fun NotificationPermissionRequester(sessionState: SessionState) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var hasAsked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* Result is not tracked in this phase — no settings UI exists yet. */ }

    val signedIn = sessionState is SessionState.StudentActive ||
        sessionState is SessionState.AdminActive
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(signedIn) {
        if (signedIn && !granted && !hasAsked) {
            hasAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
