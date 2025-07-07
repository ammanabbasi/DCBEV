package com.dealerscloud.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dealerscloud.integration.JWTAuthManager
import com.dealerscloud.ui.screens.*
import com.dealerscloud.ui.viewmodels.AuthNavigationViewModel
import javax.inject.Inject

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Login : Screen("login")
    object FirebaseLogin : Screen("firebase_login")
    object DCBEVLogin : Screen("dcbev_login")
    object Dashboard : Screen("dashboard")
    object AdminPortal : Screen("admin_portal")
    object Settings : Screen("settings")
    object Inventory : Screen("inventory")
    object Customers : Screen("customers")
    object Deals : Screen("deals")
    object Reports : Screen("reports")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Get authManager through a ViewModel that's accessible in navigation
    val authViewModel: AuthNavigationViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Start destination based on auth state
    val startDestination = if (authState.isAuthenticated) {
        Screen.Chat.route
    } else {
        Screen.FirebaseLogin.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.FirebaseLogin.route) {
            FirebaseLoginScreen(navController = navController)
        }
        
        composable(Screen.DCBEVLogin.route) {
            DCBEVLoginScreen(
                onLoginSuccess = { user ->
                    // Navigate based on user role
                    if (user.role == "Admin") {
                        navController.navigate(Screen.AdminPortal.route) {
                            popUpTo(Screen.DCBEVLogin.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.DCBEVLogin.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            // Redirect to Firebase login by default
            LaunchedEffect(Unit) {
                navController.navigate(Screen.FirebaseLogin.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
        
        composable(Screen.Chat.route) {
            if (!authState.isAuthenticated) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                }
            } else {
                EnhancedChatScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAdmin = {
                        if (authState.user?.role == "Admin") {
                            navController.navigate(Screen.AdminPortal.route)
                        }
                    }
                )
            }
        }
        
        composable(Screen.AdminPortal.route) {
            if (!authState.isAuthenticated || authState.user?.role != "Admin") {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AdminPortal.route) { inclusive = true }
                    }
                }
            } else {
                EnhancedAdminPortalScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route)
                    },
                    onNavigateToInventory = {
                        navController.navigate(Screen.Inventory.route)
                    },
                    onNavigateToCustomers = {
                        navController.navigate(Screen.Customers.route)
                    }
                )
            }
        }
        
        composable(Screen.Dashboard.route) {
            if (!authState.isAuthenticated) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            } else {
                DealersCloudDashboardScreen(
                    user = remember {
                        com.dealerscloud.integration.DealersCloudUser(
                            userId = 1,
                            username = authState.user?.email ?: "",
                            dealershipId = 1,
                            role = authState.user?.role ?: "User",
                            permissions = emptyList(),
                            accessToken = authState.accessToken ?: "",
                            dealerUrl = authState.user?.dealershipUrl ?: "",
                            dealerInfo = com.dealerscloud.integration.DealerInfo(
                                name = "DCBEV Dealership",
                                subdomain = "dcbev",
                                phone = null,
                                address = null,
                                established = null,
                                type = "Auto Dealer"
                            )
                        )
                    },
                    onNavigateToVehicles = {
                        navController.navigate(Screen.Inventory.route)
                    },
                    onNavigateToCustomers = {
                        navController.navigate(Screen.Customers.route)
                    },
                    onNavigateToDeals = {
                        navController.navigate(Screen.Deals.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // TODO: Implement remaining screens
        composable(Screen.Inventory.route) {
            // Placeholder - implement InventoryScreen
            DCBEVChatScreen()
        }
        
        composable(Screen.Customers.route) {
            // Placeholder - implement CustomersScreen
            DCBEVChatScreen()
        }
        
        composable(Screen.Deals.route) {
            // Placeholder - implement DealsScreen
            DCBEVChatScreen()
        }
        
        composable(Screen.Reports.route) {
            // Placeholder - implement ReportsScreen
            DCBEVChatScreen()
        }
    }
}