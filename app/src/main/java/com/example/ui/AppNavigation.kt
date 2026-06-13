package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.viewmodel.SslcViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(viewModel: SslcViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("home") {
            SslcHomeScreen(
                onFeatureClick = { feature ->
                    when (feature) {
                        "Ask AI" -> {
                            viewModel.updateMode("Assistant")
                            navController.navigate("chat")
                        }
                        "AI Tutor" -> {
                            viewModel.updateMode("Tutor")
                            navController.navigate("chat")
                        }
                        "Quiz Generator" -> {
                            viewModel.updateMode("Quiz")
                            navController.navigate("chat")
                        }
                        "Study Planner" -> navController.navigate("study_planner")
                        "Revision Notes" -> navController.navigate("revision_notes")
                        "Progress Dashboard" -> navController.navigate("progress_dashboard")
                        "Settings" -> navController.navigate("settings")
                    }
                },
                currentLanguage = viewModel.language,
                onLanguageToggle = { viewModel.toggleLanguage() }
            )
        }
        composable("chat") {
            SslcAppScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable("study_planner") {
            StudyPlannerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("revision_notes") {
            RevisionNotesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("progress_dashboard") {
            ProgressDashboardScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
