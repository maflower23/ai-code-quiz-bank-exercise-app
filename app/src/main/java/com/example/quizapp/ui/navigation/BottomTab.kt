package com.example.quizapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    object Banks : BottomTab(Routes.Banks.route, "题库", Icons.Filled.LibraryBooks)
    object Practice : BottomTab(Routes.Practice.route, "练习", Icons.Filled.School)
    object Records : BottomTab(Routes.Records.route, "错题", Icons.Filled.Apps)
}

val bottomTabs = listOf(BottomTab.Banks, BottomTab.Practice, BottomTab.Records)
