package com.example.foodfast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodfast.data.User
import com.example.foodfast.data.UserRole
import com.example.foodfast.ui.screens.AdminHomeScreen
import com.example.foodfast.ui.screens.BusinessHomeScreen
import com.example.foodfast.ui.screens.CartScreen
import com.example.foodfast.ui.screens.HomeScreen
import com.example.foodfast.ui.screens.LoginScreen
import com.example.foodfast.ui.screens.MenuScreen
import com.example.foodfast.ui.screens.PaymentScreen
import com.example.foodfast.ui.theme.FoodFastTheme
import com.example.foodfast.ui.viewmodel.CartViewModel

class MainActivity : ComponentActivity() {
    private val cartViewModel: CartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodFastTheme {
                MainNavigation(cartViewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: CartViewModel) {
    val navController = rememberNavController()
    var currentUser by remember { mutableStateOf<User?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        val destination = when (user.role) {
                            UserRole.ADMIN -> "admin_home"
                            UserRole.NEGOCIO -> "business_home"
                            else -> "home"
                        }
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("admin_home") {
                AdminHomeScreen(
                    currentUser = currentUser,
                    onLogout = {
                        currentUser = null
                        navController.navigate("login") {
                            popUpTo("admin_home") { inclusive = true }
                        }
                    }
                )
            }
            composable("business_home") {
                BusinessHomeScreen(
                    currentUser = currentUser,
                    onLogout = {
                        currentUser = null
                        navController.navigate("login") {
                            popUpTo("business_home") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onRestaurantClick = { restaurantId, dishName ->
                        val route = if (dishName != null) {
                            "menu/$restaurantId?dish=$dishName"
                        } else {
                            "menu/$restaurantId"
                        }
                        navController.navigate(route)
                    },
                    onViewCart = { navController.navigate("cart") },
                    onLogout = {
                        currentUser = null
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "menu/{restaurantId}?dish={dishName}",
                arguments = listOf(
                    navArgument("restaurantId") { type = NavType.StringType },
                    navArgument("dishName") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                val dishName = backStackEntry.arguments?.getString("dishName")
                MenuScreen(
                    restaurantId = restaurantId,
                    viewModel = viewModel,
                    highlightedDish = dishName,
                    onBack = { navController.popBackStack() },
                    onViewCart = { navController.navigate("cart") }
                )
            }
            composable("cart") {
                CartScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onCheckout = { navController.navigate("payment") }
                )
            }
            composable("payment") {
                PaymentScreen(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onBack = { navController.popBackStack() },
                    onPaymentSuccess = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
        }
    }
}
