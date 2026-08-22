package com.gumlapolytechnic.gpconnect.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * GP Connect brand palette.
 *
 * Anchored to the official Gumla Polytechnic emblem:
 *  - Primary indigo  #392281 is the emblem's dominant ring color (measured from the artwork).
 *  - Tertiary orange descends from the emblem accent #FE5E01, darkened for accessible contrast.
 *  - The emblem contains no teal, so the secondary teal was selected to harmonize with the
 *    indigo while staying muted and professional.
 */
object BrandColors {
    val EmblemIndigo = Color(0xFF392281)
    val EmblemOrange = Color(0xFFFE5E01)
}

// --- Light scheme -----------------------------------------------------------

val LightPrimary = BrandColors.EmblemIndigo
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE7DEFF)
val LightOnPrimaryContainer = Color(0xFF22005D)

val LightSecondary = Color(0xFF006A6E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFF9CF0F4)
val LightOnSecondaryContainer = Color(0xFF002022)

val LightTertiary = Color(0xFF9C4A00)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDCC5)
val LightOnTertiaryContainer = Color(0xFF331200)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

val LightBackground = Color(0xFFFCF9FE)
val LightOnBackground = Color(0xFF1B1A22)
val LightSurface = Color(0xFFFCF9FE)
val LightOnSurface = Color(0xFF1B1A22)
val LightSurfaceVariant = Color(0xFFE7E1F3)
val LightOnSurfaceVariant = Color(0xFF484554)

val LightOutline = Color(0xFF787489)
val LightOutlineVariant = Color(0xFFC8C4D8)

val LightInverseSurface = Color(0xFF312F3A)
val LightInverseOnSurface = Color(0xFFF4EFF7)
val LightInversePrimary = Color(0xFFCBB8FF)

// --- Dark scheme ------------------------------------------------------------
// Deliberately not an inversion of light: deep violet-black surfaces, softened
// lavender primary, and low-tone containers keep text readable on dark.

val DarkPrimary = Color(0xFFCBB8FF)
val DarkOnPrimary = Color(0xFF2F1461)
val DarkPrimaryContainer = Color(0xFF49309A)
val DarkOnPrimaryContainer = Color(0xFFE6DEFF)

val DarkSecondary = Color(0xFF80D4D8)
val DarkOnSecondary = Color(0xFF003739)
val DarkSecondaryContainer = Color(0xFF004F52)
val DarkOnSecondaryContainer = Color(0xFF9CF0F4)

val DarkTertiary = Color(0xFFFFB784)
val DarkOnTertiary = Color(0xFF4A2800)
val DarkTertiaryContainer = Color(0xFF6B3B00)
val DarkOnTertiaryContainer = Color(0xFFFFDCC5)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF131218)
val DarkOnBackground = Color(0xFFE5E1EC)
val DarkSurface = Color(0xFF131218)
val DarkOnSurface = Color(0xFFE5E1EC)
val DarkSurfaceVariant = Color(0xFF484554)
val DarkOnSurfaceVariant = Color(0xFFC9C5D4)

val DarkOutline = Color(0xFF938F9F)
val DarkOutlineVariant = Color(0xFF484554)

val DarkInverseSurface = Color(0xFFE5E1EC)
val DarkInverseOnSurface = Color(0xFF312F3A)
val DarkInversePrimary = Color(0xFF4A3290)
