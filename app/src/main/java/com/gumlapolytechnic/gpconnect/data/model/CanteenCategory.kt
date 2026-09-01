package com.gumlapolytechnic.gpconnect.data.model

/**
 * Canteen menu category.
 *
 * The enum name is the canonical identifier. Display name is presentation-only.
 * [enabled] controls visibility in the student menu.
 * [displayOrder] defines the sort order across categories.
 */
data class CanteenCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)