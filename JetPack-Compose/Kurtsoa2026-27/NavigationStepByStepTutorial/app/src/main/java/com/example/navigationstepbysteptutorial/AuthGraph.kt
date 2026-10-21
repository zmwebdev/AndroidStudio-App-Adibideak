package com.example.navigationstepbysteptutorial

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onDone = { navController.popBackStack() })
        }
    }
}