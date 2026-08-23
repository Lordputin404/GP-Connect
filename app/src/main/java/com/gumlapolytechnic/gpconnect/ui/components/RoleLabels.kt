package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.UserRole

/** Display labels for roles and modules (shared by admin screens). */

@Composable
fun roleLabel(role: UserRole): String = stringResource(
    when (role) {
        UserRole.SUPER_ADMIN -> R.string.role_super_admin
        UserRole.CANTEEN_ADMIN -> R.string.role_canteen_admin
        UserRole.LIBRARY_ADMIN -> R.string.role_library_admin
        UserRole.FACULTY_ADMIN -> R.string.role_faculty_admin
        UserRole.FACILITY_ADMIN -> R.string.role_facility_admin
        UserRole.STUDENT -> R.string.profile_role_student
    },
)

@Composable
fun moduleLabel(module: AdminModule): String = stringResource(
    when (module) {
        AdminModule.LIBRARY -> R.string.module_library
        AdminModule.CANTEEN -> R.string.module_canteen
        AdminModule.FACULTY -> R.string.module_faculty
        AdminModule.FACILITY -> R.string.module_facility
        AdminModule.GLOBAL -> R.string.module_global
    },
)
