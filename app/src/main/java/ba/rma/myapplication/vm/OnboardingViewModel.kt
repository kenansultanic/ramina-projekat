package ba.rma.myapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the first-run onboarding screen.
 *
 * Its job is small: give the UI the list of national teams to pick a favorite from, and remember the
 * user's choice (plus the fact that onboarding is now done) when they finish.
 *
 * We extend AndroidViewModel so we can grab the Application context, which we need both for the
 * shared repository singleton and for SettingsManager.
 */
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    // The one shared repository (single source of truth) used by the whole app.
    private val repo = CardRepository.get(app)

    /**
     * The list of teams ("reprezentacija") the user can choose from. We read the whole album and map
     * it down to the distinct, alphabetically sorted list of team names. If the catalog is empty
     * (e.g. very first launch before the download finished) this is simply an empty list and the UI
     * shows a friendly empty state.
     */
    val teams: StateFlow<List<String>> =
        repo.observeAlbum()
            .map { cards ->
                cards.map { card -> card.reprezentacija }
                    .distinct()
                    .sorted()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    init {
        // Best-effort catalog download so the team list can populate on first run. refreshCatalog()
        // never throws and never clears the cache, so this is safe to fire-and-forget.
        viewModelScope.launch {
            repo.refreshCatalog()
        }
    }

    /**
     * Saves the user's onboarding result and marks onboarding as complete. The chosen [team] may be
     * empty when the user pressed "Preskoči" (skip) without picking anything — that is fine, we just
     * store an empty favorite team. Either way onboardingDone becomes true so the splash screen sends
     * the user straight to the home screen from now on.
     */
    fun finish(team: String) {
        val settings = SettingsManager(getApplication())
        settings.favoriteTeam = team
        settings.onboardingDone = true
    }
}
