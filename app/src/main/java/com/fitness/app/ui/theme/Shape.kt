package com.fitness.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 现代圆角体系：大圆角 + 弧形层次
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// 通用圆角预设
val CardShape = RoundedCornerShape(20.dp)
val ChipShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(50)
val ImageShape = RoundedCornerShape(14.dp)
