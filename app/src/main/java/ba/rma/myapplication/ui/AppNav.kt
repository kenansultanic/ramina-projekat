package ba.rma.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ba.rma.myapplication.data.ThemeMode

/**
 * Central place that lists every navigation route (screen address) in the app.
 *
 * Keeping the route strings here as constants means we never type the same string in two places and
 * risk a typo. The detail route carries one argument: the player id.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"

    // Name of the argument passed to the detail screen. This MUST match DetailViewModel.ARG_PLAYER_ID.
    const val ARG_PLAYER_ID = "playerId"

    // The pattern Navigation matches against, with the argument placeholder in braces.
    const val DETAIL_PATTERN = "detail/{playerId}"

    // Helper to build a concrete detail route for a given player id (fills in the placeholder).
    fun detail(id: Int): String = "detail/$id"
}

/**
 * The outer navigation graph for the whole app.
 *
 * It owns the NavController and wires every top-level destination together. The theme state lives
 * above this (in MainActivity), so we accept the current [themeMode] plus a callback to change it.
 * The callback is forwarded down to the Settings screen so the user can switch theme live.
 */
@Composable
fun AppNav(themeMode: ThemeMode, onThemeChanged: (ThemeMode) -> Unit) {
    // The NavController remembers where we are and lets us move between screens.
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {

        // Splash: shows the brand for a moment, then jumps to onboarding or home.
        composable(Routes.SPLASH) {
            SplashScreen(
                onGoToOnboarding = {
                    nav.navigate(Routes.ONBOARDING) {
                        // Remove splash from the back stack so Back doesn't return to it.
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoToHome = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding: first-run team picker. When done, go to home and drop onboarding.
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Home: the main tabbed shell (album, packs, stats, favorites, suggestions).
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        // Settings: theme/sort/density. Changing theme calls back up to MainActivity.
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onThemeChanged = onThemeChanged
            )
        }

        // Detail: shows one player. The {playerId} part of the route is an Int argument.
        composable(
            route = Routes.DETAIL_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_PLAYER_ID) { type = NavType.IntType })
        ) {
            DetailScreen(onBack = { nav.popBackStack() })
        }
    }
}
