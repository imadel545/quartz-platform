package com.quartz.platform.presentation.navigation

sealed class QuartzDestination(val route: String) {
    data object Home : QuartzDestination("home")
}
