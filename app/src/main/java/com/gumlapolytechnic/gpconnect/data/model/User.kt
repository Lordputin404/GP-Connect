package com.gumlapolytechnic.gpconnect.data.model

/**
 * Application user. Roles drive which experience is shown — the single APK
 * serves both students and admins (admin tooling arrives in a later phase).
 */
data class User(
    val id: String,
    val name: String,
    val rollNo: String,
    val course: String,
    val semester: Int,
    val role: UserRole,
    val department: String,
)

enum class UserRole {
    STUDENT,
    ADMIN,
}
