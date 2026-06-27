package ba.rma.myapplication.data

import android.content.Context

/** How the album list is sorted. */
enum class SortOrder { NUMBER, NAME, ACQUIRED }

/** How tightly cards are packed in the grid. The number is the minimum cell width in dp. */
enum class GridDensity(val minCellDp: Int) { COMFORTABLE(160), COMPACT(120) }

/** App color theme choice. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Tiny wrapper around SharedPreferences for the app's saved settings/preferences.
 *
 * We use SharedPreferences (not Room) because these are a handful of simple values, which is exactly
 * what SharedPreferences is for. Each property reads/writes one key. Enums are stored by their
 * name() and read back with a safe fallback to the default if the stored text is somehow invalid.
 */
class SettingsManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("slicice_prefs", Context.MODE_PRIVATE)

    /** Has the user finished the first-run onboarding? Splash uses this to decide where to go. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) {
            prefs.edit().putBoolean("onboarding_done", value).apply()
        }

    /** The team the user picked during onboarding ("" if none/skipped). */
    var favoriteTeam: String
        get() = prefs.getString("favorite_team", "") ?: ""
        set(value) {
            prefs.edit().putString("favorite_team", value).apply()
        }

    /** Default sort order for the album list. */
    var defaultSort: SortOrder
        get() = readEnum("default_sort", SortOrder.NUMBER) { SortOrder.valueOf(it) }
        set(value) {
            prefs.edit().putString("default_sort", value.name).apply()
        }

    /** Grid density (how many cards per row, roughly). */
    var gridDensity: GridDensity
        get() = readEnum("grid_density", GridDensity.COMFORTABLE) { GridDensity.valueOf(it) }
        set(value) {
            prefs.edit().putString("grid_density", value.name).apply()
        }

    /** Light / dark / follow-system theme. */
    var themeMode: ThemeMode
        get() = readEnum("theme_mode", ThemeMode.SYSTEM) { ThemeMode.valueOf(it) }
        set(value) {
            prefs.edit().putString("theme_mode", value.name).apply()
        }

    /** If true, the album hides missing stickers by default. */
    var showOnlyOwned: Boolean
        get() = prefs.getBoolean("show_only_owned", false)
        set(value) {
            prefs.edit().putBoolean("show_only_owned", value).apply()
        }

    /**
     * Helper that reads an enum stored as text. If the key is missing or the text does not match any
     * enum value, it returns [default] instead of crashing.
     */
    private fun <T> readEnum(key: String, default: T, convert: (String) -> T): T {
        val text = prefs.getString(key, null)
        if (text == null) {
            return default
        }
        return try {
            convert(text)
        } catch (e: Exception) {
            default
        }
    }
}
