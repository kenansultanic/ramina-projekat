package ba.rma.myapplication.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ba.rma.myapplication.data.SettingsManager
import ba.rma.myapplication.data.SortOrder
import ba.rma.myapplication.data.ThemeMode

/**
 * ViewModel for the Settings screen.
 *
 * It exposes the four configurable preferences as Compose state (so the UI recomposes when they
 * change) and persists every change to [SettingsManager] (backed by SharedPreferences).
 *
 * We use mutableStateOf with `private set` so only the ViewModel can mutate the state; the screen
 * changes values by calling the dedicated setter functions below.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    // Small wrapper around SharedPreferences where the settings actually live.
    private val settings = SettingsManager(app)

    /** Light / dark / follow-system theme. */
    var themeMode by mutableStateOf(settings.themeMode)
        private set

    /** Persist + update the theme mode. */
    fun updateThemeMode(value: ThemeMode) {
        settings.themeMode = value
        themeMode = value
    }
}
