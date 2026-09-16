package ba.rma.myapplication.data

import android.content.Context

/** How the album list is sorted. */
enum class SortOrder { NUMBER, NAME, ACQUIRED }

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

    /** Light / dark / follow-system theme. */
    var themeMode: ThemeMode
        get() = readEnum("theme_mode", ThemeMode.SYSTEM) { ThemeMode.valueOf(it) }
        set(value) {
            prefs.edit().putString("theme_mode", value.name).apply()
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
