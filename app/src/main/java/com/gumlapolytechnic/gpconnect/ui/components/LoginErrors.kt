package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult

/**
 * Maps a login failure to a friendly, user-facing message. Raw Firebase
 * exception text is never surfaced.
 */
@Composable
fun loginErrorMessage(error: LoginResult, adminForm: Boolean): String = when (error) {
    LoginResult.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    LoginResult.AccountNotConfigured -> stringResource(R.string.error_account_not_configured)
    LoginResult.AccountDisabled -> stringResource(R.string.error_account_disabled)
    LoginResult.WrongRole -> stringResource(
        if (adminForm) R.string.error_not_admin_account else R.string.error_not_student_account,
    )
    LoginResult.NetworkError -> stringResource(R.string.error_network)
    LoginResult.RateLimited -> stringResource(R.string.error_rate_limited)
    LoginResult.Success -> ""
}
