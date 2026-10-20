package com.tenge.budget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radii tuned to match the web app (rounded-xl ≈ 12dp for buttons/inputs,
// rounded-2xl ≈ 16dp for cards) rather than the previous, rounder 24/32dp set.
val TengeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
