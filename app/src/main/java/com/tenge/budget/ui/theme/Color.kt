package com.tenge.budget.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tenge Tracker palette — matches the web app 1:1 (see tailwind.config.js:
 * ink/paper/brand/accent/danger/info) so the Android app reads as the same
 * product rather than a reskinned template. Warm off-white canvas, near-black
 * "ink" for text and the highest-contrast surfaces (dark cards, active nav),
 * a confident green as the one brand color, amber/coral as the two semantic
 * accents.
 */

// Brand — teal-green scale, 50 (lightest) to 900 (darkest). Mirrors
// tailwind.config.js `brand` exactly.
val Brand50 = Color(0xFFEEFBF6)
val Brand100 = Color(0xFFD3F3E7)
val Brand200 = Color(0xFFA6E7CF)
val Brand300 = Color(0xFF6FD5B3)
val Brand400 = Color(0xFF3EBE96)
val Brand500 = Color(0xFF1FA37C)
val Brand600 = Color(0xFF158265)
val Brand700 = Color(0xFF126853)
val Brand800 = Color(0xFF0F5343)
val Brand900 = Color(0xFF0B3E32)

// Used for the deep end of primary-color gradients (balance/hero cards) —
// replaces the old "IndigoDeep" role now that primary is brand green.
val BrandGradientEnd = Brand800

// Ink / paper — the near-black text/surface color and the warm off-white
// canvas, exactly as in the web app.
val Ink = Color(0xFF12181B)
val InkSecondary = Color(0xFF6B7278)
val Paper = Color(0xFFF7F5F0)
val CardSurface = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEFEEE8)
val OutlineLight = Color(0xFFE4E2DA)

val InkDarkBg = Color(0xFF0D1214)
val CardSurfaceDark = Color(0xFF1A2023)
val SurfaceVariantDark = Color(0xFF222A2D)
val ParchmentText = Color(0xFFF2F1EC)
val ParchmentTextSecondary = Color(0xFFA3A9A6)
val OutlineDark = Color(0xFF2E3538)

// Semantic accents — one for "over budget / overdue / expense", one for
// "warning / near limit", one informational blue. Matches accent/danger/info
// in tailwind.config.js.
val Accent = Color(0xFFE8A33D) // amber — "near limit" / warnings
val AccentContainerLight = Color(0xFFFCEACB)
val AccentContainerDark = Color(0xFF5C4419)

val Danger = Color(0xFFD6533C)
val DangerDark = Color(0xFFFF8A70)
val DangerContainerLight = Color(0xFFFAE1DC)
val DangerContainerDark = Color(0xFF5C2A1F)

val Info = Color(0xFF3B7DD8)
val InfoDark = Color(0xFF7FAEEE)

// Income/expense are just semantic aliases of brand/danger — money that
// comes in reads as brand green, money going out reads as danger coral, same
// as the web app's tx.type === "INCOME" ? text-brand-700 : text-danger.
val IncomeGreen = Brand600
val IncomeGreenDark = Brand300
val IncomeContainerLight = Brand50
val IncomeContainerDark = Brand900
val ExpenseCoral = Danger
val ExpenseCoralDark = DangerDark
val ExpenseContainerLight = DangerContainerLight
val ExpenseContainerDark = DangerContainerDark

val ErrorRed = Danger
val ErrorRedDark = DangerDark

/**
 * Rotating palette for category badges/icons so each category still reads as
 * its own visual identity — recolored to sit comfortably next to the brand
 * green instead of the old indigo/violet set.
 */
data class CategoryPalette(val accent: Color, val container: Color, val onContainer: Color)

val categoryPalettesLight = listOf(
    CategoryPalette(Brand600, Brand50, Brand800),                              // brand green
    CategoryPalette(Color(0xFF3B7DD8), Color(0xFFE1EBFA), Color(0xFF1F4E8C)),  // info blue
    CategoryPalette(Color(0xFFE8A33D), AccentContainerLight, Color(0xFF8A5B00)), // amber
    CategoryPalette(Color(0xFFD6533C), DangerContainerLight, Color(0xFF8C3423)), // coral
    CategoryPalette(Color(0xFF8B6CF6), Color(0xFFEBE6FE), Color(0xFF4F3BA8)),  // violet
    CategoryPalette(Color(0xFF1FBFA0), Color(0xFFDCF8F1), Color(0xFF126853)),  // teal
    CategoryPalette(Color(0xFFB08A3E), Color(0xFFF1E8D3), Color(0xFF6B4E1E)),  // bronze
    CategoryPalette(Color(0xFF5B8C3E), Color(0xFFE6F1DA), Color(0xFF3B5C28)),  // olive
    CategoryPalette(Color(0xFFCF6BA0), Color(0xFFFAE3EF), Color(0xFF8C3E68)),  // rose
    CategoryPalette(Color(0xFF126853), Brand100, Brand900)                     // deep brand
)

val categoryPalettesDark = listOf(
    CategoryPalette(Brand300, Brand800, Brand50),
    CategoryPalette(InfoDark, Color(0xFF29405E), Color(0xFFDCEBFC)),
    CategoryPalette(Color(0xFFFFC961), AccentContainerDark, Color(0xFFFFEBC2)),
    CategoryPalette(DangerDark, DangerContainerDark, Color(0xFFFFDCD3)),
    CategoryPalette(Color(0xFFB4A0FF), Color(0xFF3A3470), Color(0xFFE3DEFF)),
    CategoryPalette(Color(0xFF4FD9BC), Color(0xFF1D4A40), Color(0xFFD3F7EE)),
    CategoryPalette(Color(0xFFD9BC7C), Color(0xFF4E3E1E), Color(0xFFF1E4C6)),
    CategoryPalette(Color(0xFFA9D66E), Color(0xFF3B4A22), Color(0xFFEAF5D6)),
    CategoryPalette(Color(0xFFEAA0C6), Color(0xFF4E2A3A), Color(0xFFFFDDEC)),
    CategoryPalette(Brand400, Brand900, Brand100)
)
