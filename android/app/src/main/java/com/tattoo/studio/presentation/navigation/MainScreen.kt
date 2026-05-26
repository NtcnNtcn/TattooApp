package com.tattoo.studio.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tattoo.studio.presentation.feed.FeedScreen
import com.tattoo.studio.presentation.profile.ProfileScreen
import com.tattoo.studio.presentation.social.FavoritesScreen
import com.tattoo.studio.presentation.upload.UploadWorkScreen
import com.tattoo.studio.presentation.moderation.ModerationScreen
import com.tattoo.studio.presentation.applications.ClientApplicationsScreen
import com.tattoo.studio.presentation.masters.MastersListScreen
import com.tattoo.studio.presentation.theme.*
import androidx.hilt.navigation.compose.hiltViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Feed : BottomNavItem("main_feed", Icons.Outlined.GridView, "ЛЕНТА")
    object Favorites : BottomNavItem("main_favorites", Icons.Outlined.FavoriteBorder, "ИЗБРАННОЕ")
    object Subscriptions : BottomNavItem("main_subscriptions", Icons.Outlined.Subscriptions, "ПОДПИСКИ")
    object Upload : BottomNavItem("main_upload", Icons.Outlined.AddBox, "ЗАГРУЗИТЬ")
    object Profile : BottomNavItem("main_profile", Icons.Outlined.PersonOutline, "ПРОФИЛЬ")
    
    // Admin Items
    object Moderation : BottomNavItem("main_moderation", Icons.Outlined.FactCheck, "МОДЕРАЦИЯ")
    object Applications : BottomNavItem("main_applications", Icons.Outlined.Assignment, "ЗАЯВКИ")
    object Masters : BottomNavItem("main_masters", Icons.Outlined.Groups, "МАСТЕРЫ")
}

@Composable
fun MainScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToModeration: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToMasters: () -> Unit,
    onNavigateToMasterDetail: (Int) -> Unit,
    onNavigateToWorkDetail: (Int) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val userRole by viewModel.userRole.collectAsState()
    val isMaster = userRole?.lowercase() == "master"
    val isAdmin = userRole?.lowercase() == "admin"

    val navController = rememberNavController()
    val items = when {
        isAdmin -> listOf(
            BottomNavItem.Moderation,
            BottomNavItem.Applications,
            BottomNavItem.Masters,
            BottomNavItem.Profile
        )
        isMaster -> listOf(
            BottomNavItem.Feed,
            BottomNavItem.Favorites,
            BottomNavItem.Upload,
            BottomNavItem.Profile
        )
        else -> listOf(
            BottomNavItem.Feed,
            BottomNavItem.Favorites,
            BottomNavItem.Profile
        )
    }

    val startDestination = if (isAdmin) BottomNavItem.Moderation.route else BottomNavItem.Feed.route

    Scaffold(
        bottomBar = {
            MagnumBottomBar(
                items = items,
                navController = navController
            )
        },
        containerColor = Background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Feed.route) {
                FeedScreen(onNavigateToWorkDetail = onNavigateToWorkDetail)
            }
            composable(BottomNavItem.Favorites.route) {
                FavoritesScreen(onWorkClick = onNavigateToWorkDetail)
            }
            composable(BottomNavItem.Upload.route) {
                UploadWorkScreen(
                    onUploadSuccess = { navController.navigate(BottomNavItem.Feed.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogoutSuccess = onLogoutSuccess,
                    onNavigateToModeration = onNavigateToModeration,
                    onNavigateToReports = onNavigateToReports,
                    onNavigateToApplications = onNavigateToApplications,
                    onNavigateToMasters = onNavigateToMasters,
                    onNavigateToEditProfile = onNavigateToEditProfile
                )
            }
            
            // Admin routes inside internal NavHost
            composable(BottomNavItem.Moderation.route) {
                ModerationScreen()
            }
            composable(BottomNavItem.Applications.route) {
                ClientApplicationsScreen(onBack = { navController.popBackStack() })
            }
            composable(BottomNavItem.Masters.route) {
                MastersListScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = onNavigateToMasterDetail
                )
            }
        }
    }
}

@Composable
fun MagnumBottomBar(
    items: List<BottomNavItem>,
    navController: androidx.navigation.NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column {
        // Top divider
        HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Active indicator line above icon
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(if (isSelected) Primary else android.graphics.Color.TRANSPARENT.let { 
                                androidx.compose.ui.graphics.Color.Transparent 
                            })
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) Primary else Outline
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = if (isSelected) Primary else Outline
                        )
                    )
                }
            }
        }
    }
}
