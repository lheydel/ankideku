package com.ankideku.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object PanelSizes {
    val headerHeight = 81.dp

    // Queue panel (left) - responsive constraints
    val queuePanelMinWidth = 260.dp
    val queuePanelMaxWidth = 360.dp
    val queuePanelWeight = 0.22f

    // Sidebar panel (right) - responsive constraints
    val sidebarMinWidth = 380.dp
    val sidebarMaxWidth = 520.dp
    val sidebarWeight = 0.30f

    // Center panel
    val minComparisonWidth = 400.dp
}

object AnimationDurations {
    const val slideUp = 400  // 0.4s
    const val fadeIn = 300  // 0.3s
    const val slideIn = 300  // 0.3s
    const val quick = 150
}

object IconSizes {
    val small = 16.dp
    val medium = 20.dp
    val large = 24.dp
    val xlarge = 32.dp
}
