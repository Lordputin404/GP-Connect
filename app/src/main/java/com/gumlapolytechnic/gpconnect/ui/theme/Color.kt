package com.gumlapolytechnic.gpconnect.ui.theme

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
    const val EmblemIndigo = 0xFF392281
    const val EmblemOrange = 0xFFFE5E01
}

// --- Light scheme -----------------------------------------------------------

val LightPrimary = BrandColors.EmblemIndigo
val LightOnPrimary = 0xFFFFFFFF
val LightPrimaryContainer = 0xFFE7DEFF
val LightOnPrimaryContainer = 0xFF22005D

val LightSecondary = 0xFF006A6E
val LightOnSecondary = 0xFFFFFFFF
val LightSecondaryContainer = 0xFF9CF0F4
val LightOnSecondaryContainer = 0xFF002022

val LightTertiary = 0xFF9C4A00
val LightOnTertiary = 0xFFFFFFFF
val LightTertiaryContainer = 0xFFFFDCC5
val LightOnTertiaryContainer = 0xFF331200

val LightError = 0xFFB3261E
val LightOnError = 0xFFFFFFFF
val LightErrorContainer = 0xFFF9DEDC
val LightOnErrorContainer = 0xFF410E0B

val LightBackground = 0xFFFCF9FE
val LightOnBackground = 0xFF1B1A22
val LightSurface = 0xFFFCF9FE
val LightOnSurface = 0xFF1B1A22
val LightSurfaceVariant = 0xFFE7E1F3
val LightOnSurfaceVariant = 0xFF484554

val LightOutline = 0xFF787489
val LightOutlineVariant = 0xFFC8C4D8

val LightInverseSurface = 0xFF312F3A
val LightInverseOnSurface = 0xFFF4EFF7
val LightInversePrimary = 0xFFCBB8FF

// --- Dark scheme ------------------------------------------------------------
// Deliberately not an inversion of light: deep violet-black surfaces, softened
// lavender primary, and low-tone containers keep text readable on dark.

val DarkPrimary = 0xFFCBB8FF
val DarkOnPrimary = 0xFF2F1461
val DarkPrimaryContainer = 0xFF49309A
val DarkOnPrimaryContainer = 0xFFE6DEFF

val DarkSecondary = 0xFF80D4D8
val DarkOnSecondary = 0xFF003739
val DarkSecondaryContainer = 0xFF004F52
val DarkOnSecondaryContainer = 0xFF9CF0F4

val DarkTertiary = 0xFFFFB784
val DarkOnTertiary = 0xFF4A2800
val DarkTertiaryContainer = 0xFF6B3B00
val DarkOnTertiaryContainer = 0xFFFFDCC5

val DarkError = 0xFFFFB4AB
val DarkOnError = 0xFF690005
val DarkErrorContainer = 0xFF93000A
val DarkOnErrorContainer = 0xFFFFDAD6

val DarkBackground = 0xFF131218
val DarkOnBackground = 0xFFE5E1EC
val DarkSurface = 0xFF131218
val DarkOnSurface = 0xFFE5E1EC
val DarkSurfaceVariant = 0xFF484554
val DarkOnSurfaceVariant = 0xFFC9C5D4

val DarkOutline = 0xFF938F9F
val DarkOutlineVariant = 0xFF484554

val DarkInverseSurface = 0xFFE5E1EC
val DarkInverseOnSurface = 0xFF312F3A
val DarkInversePrimary = 0xFF4A3290
