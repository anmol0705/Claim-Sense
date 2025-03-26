package android.saswat.claimsense.navigation

import android.saswat.claimsense.ui.Screens
import android.saswat.claimsense.ui.dashboard.DashboardScreen
import android.saswat.claimsense.ui.main.MainScreen
import android.saswat.claimsense.ui.signInSignUp.SignInScreen
import android.saswat.claimsense.ui.signInSignUp.SignUpScreen
import android.saswat.claimsense.ui.vehicles.VehiclesScreen
import android.saswat.claimsense.viewmodel.AuthViewModel
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        startDestination = Screens.Login.route,
        navController = navController
    ) {
        // Sign In Screen
        composable(
            route = Screens.Login.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screens.MainScreen.route) {
                        popUpTo(Screens.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Screens.SignUp.route)
                }
            )
        }

        // Sign Up Screen
        composable(
            route = Screens.SignUp.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screens.MainScreen.route) {
                        popUpTo(Screens.SignUp.route) { inclusive = true }
                    }
                },
                onSignInClick = {
                    navController.popBackStack()
                }
            )
        }

        // Main Screen
        composable(
            route = Screens.MainScreen.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            MainScreen(
                onSignOut = {
                    navController.navigate(Screens.Login.route) {
                        popUpTo(Screens.MainScreen.route) { inclusive = true }
                    }
                },

            )
        }

        composable(route = Screens.Dashboard.route) {
            val authViewModel = viewModel<AuthViewModel>()
            DashboardScreen(
                onMenuClick = {
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }

        composable(route = Screens.Vehicles.route) {
            VehiclesScreen(
                onMenuClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screens.Claims.route) {
            Text("Claims History Screen") // TODO: Implement Claims Screen
        }
    }
}