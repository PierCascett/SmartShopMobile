/**
 * Color.kt
 *
 * MVVM: View Layer - Palette colori Material Design
 *
 * FUNZIONAMENTO:
 * - Definisce palette colori per tema Light e Dark
 * - Primary: Verde (brand supermercato)
 * - Secondary: Giallo/Arancione (accent)
 * - Tertiary: Teal (complementare)
 * - Container colors: varianti chiare per superfici
 *
 * PATTERN MVVM:
 * - Theme: configurazione visuale globale
 * - Material 3 Color System
 * - Accessibilità: contrast ratio WCAG
 * - Dark mode support
 */
package it.unito.smartshopmobile.ui.theme


import androidx.compose.ui.graphics.Color

// Green / Yellow primary palette + complementary teal
// Richer, darker green and orange-leaning yellow
// Light theme
val GreenPrimaryLight = Color(0xFF1B5E20) // Green 900 (più scuro)
val YellowSecondaryLight = Color(0xFFFF8F00) // Amber 800 (verso arancione)
val TealTertiaryLight = Color(0xFF00796B) // Teal 700

val OnPrimaryLight = Color(0xFFFFFFFF)
val OnSecondaryLight = Color(0xFF1A1A1A)
val OnTertiaryLight = Color(0xFFFFFFFF)

val PrimaryContainerLight = Color(0xFFA5D6A7) // verde chiaro per superfici
val OnPrimaryContainerLight = Color(0xFF0A1F10)
val SecondaryContainerLight = Color(0xFFFFD180) // arancio chiaro per superfici
val OnSecondaryContainerLight = Color(0xFF1A1A1A)
val TertiaryContainerLight = Color(0xFFB2DFDB)
val OnTertiaryContainerLight = Color(0xFF00332C)

// Dark theme
val GreenPrimaryDark = Color(0xFF4CAF50) // più visibile su sfondo scuro
val YellowSecondaryDark = Color(0xFFFFA000) // Amber 700 (più scuro)
val TealTertiaryDark = Color(0xFF26A69A)

val OnPrimaryDark = Color(0xFF00150A)
val OnSecondaryDark = Color(0xFF1A1A1A)
val OnTertiaryDark = Color(0xFF001A16)

val PrimaryContainerDark = Color(0xFF1B5E20)
val OnPrimaryContainerDark = Color(0xFFA5D6A7)
val SecondaryContainerDark = Color(0xFFFF8F00)
val OnSecondaryContainerDark = Color(0xFF1A1A1A)
val TertiaryContainerDark = Color(0xFF00695C)
val OnTertiaryContainerDark = Color(0xFFB2DFDB)
