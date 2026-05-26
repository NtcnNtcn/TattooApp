package com.tattoo.studio.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tattoo.studio.presentation.feed.WorkDetailScreen
import com.tattoo.studio.presentation.profile.EditProfileScreen
import com.tattoo.studio.presentation.auth.*
import androidx.navigation.toRoute
import com.tattoo.studio.presentation.applications.ClientApplicationsScreen
import com.tattoo.studio.presentation.masters.MasterDetailScreen
import com.tattoo.studio.presentation.masters.MastersListScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(MainRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) },
                onLoginSuccess = { 
                    navController.navigate(MainRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToVerification = { email -> 
                    navController.navigate(VerificationRoute(email, "registration"))
                }
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { email -> 
                    navController.navigate(VerificationRoute(email, "registration"))
                }
            )
        }
        composable<MainRoute> {
            MainScreen(
                onNavigateToUpload = { navController.navigate(UploadWorkRoute) },
                onNavigateToModeration = { navController.navigate(ModerationRoute) },
                onNavigateToReports = { navController.navigate(ReportsRoute) },
                onNavigateToApplications = { navController.navigate(ClientApplicationsRoute) },
                onNavigateToMasters = { navController.navigate(MastersListRoute) },
                onNavigateToMasterDetail = { id -> navController.navigate(MasterDetailRoute(id)) },
                onNavigateToWorkDetail = { id -> navController.navigate(WorkDetailRoute(id)) },
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onLogoutSuccess = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<UploadWorkRoute> {
            com.tattoo.studio.presentation.upload.UploadWorkScreen(
                onUploadSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<EditProfileRoute> {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVerification = { email -> 
                    navController.navigate(VerificationRoute(email, "email_change"))
                },
                onDeleteSuccess = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<VerificationRoute> { backStackEntry ->
            val route: VerificationRoute = backStackEntry.toRoute()
            VerificationScreen(
                email = route.email,
                type = route.type,
                onVerified = {
                    if (route.type == "registration") {
                        navController.navigate(MainRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onNavigateToReset = { email -> navController.navigate(ResetPasswordRoute(email)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ResetPasswordRoute> { backStackEntry ->
            val route: ResetPasswordRoute = backStackEntry.toRoute()
            ResetPasswordScreen(
                email = route.email,
                onSuccess = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ModerationRoute> {
            com.tattoo.studio.presentation.moderation.ModerationScreen()
        }
        composable<ReportsRoute> {
            com.tattoo.studio.presentation.reports.ReportsScreen()
        }
        composable<ClientApplicationsRoute> {
            ClientApplicationsScreen(onBack = { navController.popBackStack() })
        }
        composable<MastersListRoute> {
            MastersListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(MasterDetailRoute(id)) }
            )
        }
        composable<MasterDetailRoute> { backStackEntry ->
            val route: MasterDetailRoute = backStackEntry.toRoute()
            MasterDetailScreen(
                masterId = route.id,
                onBack = { navController.popBackStack() },
                onNavigateToWorkDetail = { id -> navController.navigate(WorkDetailRoute(id)) }
            )
        }
        composable<WorkDetailRoute> { backStackEntry ->
            val route: WorkDetailRoute = backStackEntry.toRoute()
            WorkDetailScreen(
                workId = route.id,
                onBack = { navController.popBackStack() },
                onNavigateToMaster = { id -> navController.navigate(MasterDetailRoute(id)) }
            )
        }
    }
}
