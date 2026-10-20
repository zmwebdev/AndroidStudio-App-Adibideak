package com.example.navigationstepbysteptutorial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Routes.HOME, Icons.Default.Home),
    BottomNavItem("Settings", Routes.SETTINGS, Icons.Default.Settings),
)