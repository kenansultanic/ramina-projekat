package ba.rma.myapplication.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ba.rma.myapplication.data.GridDensity
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

    /** Default sort order for the album list. */
    var sortOrder by mutableStateOf(settings.defaultSort)
        private set

    /** How tightly cards are packed in the album grid. */
    var gridDensity by mutableStateOf(settings.gridDensity)
        private set

    /** Light / dark / follow-system theme. */
    var themeMode by mutableStateOf(settings.themeMode)
        private set

    /** When true, the album hides missing stickers by default. */
    var showOnlyOwned by mutableStateOf(settings.showOnlyOwned)
        private set

    /** Persist + update the default sort order. */
    fun setSortOrder(value: SortOrder) {
        settings.defaultSort = value
        sortOrder = value
    }

    /** Persist + update the grid density. */
    fun updateGridDensity(value: GridDensity) {
        settings.gridDensity = value
        gridDensity = value
    }

    /** Persist + update the theme mode. */
    fun setThemeMode(value: ThemeMode) {
        settings.themeMode = value
        themeMode = value
    }

    /** Persist + update the "show only owned" toggle. */
    fun setShowOnlyOwned(value: Boolean) {
        settings.showOnlyOwned = value
        showOnlyOwned = value
    }
}
