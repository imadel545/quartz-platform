package com.quartz.platform.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quartz.platform.presentation.home.HomeRoute

@Composable
fun QuartzNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = QuartzDestination.Home.route
    ) {
        composable(QuartzDestination.Home.route) {
            HomeRoute()
        }
    }
}
