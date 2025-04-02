package android.saswat.claimsense.ui.screens

sealed class Screens(val route: String) {
    object Login : Screens("login")
    object SignUp : Screens("sign_up")
    object MainScreen : Screens("main_screen")
    object Dashboard : Screens("dashboard")
    object Vehicles : Screens("vehicles")
    object Claims : Screens("claims")
    object Profile : Screens("profile")
    object RiskScore : Screens("risk_score")
    object Chat : Screens("chat") // Add this new route
}
