package ba.rma.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ba.rma.myapplication.data.SettingsManager
import ba.rma.myapplication.data.ThemeMode
import ba.rma.myapplication.ui.AppNav
import ba.rma.myapplication.ui.theme.MyApplicationTheme

/**
 * The single Activity of the app. It sets up edge-to-edge drawing, owns the live theme state, and
 * hosts the whole navigation graph via AppNav.
 *
 * The theme choice (light/dark/follow-system) lives here as compose state so that when the user
 * changes it in Settings the whole UI recomposes with the new theme immediately, without restarting.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Start from the saved preference; updated live via the onThemeChanged callback below.
            var themeMode by remember { mutableStateOf(SettingsManager(this).themeMode) }

            // Translate our 3-way theme choice into the boolean MyApplicationTheme expects.
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // dynamicColor=false so our branded purple scheme is used consistently on all devices.
            MyApplicationTheme(darkTheme = dark, dynamicColor = false) {
                AppNav(
                    themeMode = themeMode,
                    onThemeChanged = { themeMode = it }
                )
            }
        }
    }
}
