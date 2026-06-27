package ba.rma.myapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.AlbumCard
import ba.rma.myapplication.data.CardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the single-player detail screen.
 *
 * The player id arrives through navigation as an Int argument. We read it from the
 * [SavedStateHandle] (which the default `viewModel()` factory fills automatically for us). From that
 * id we observe ONE card out of the repository's single source of truth, so the detail screen stays
 * in sync with the rest of the app (e.g. toggling the favorite here updates the album too).
 *
 * @param app the application (needed to reach the shared repository singleton).
 * @param savedState holds the navigation arguments, including the player id.
 */
class DetailViewModel(
    app: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(app) {

    // The shared repository (single source of truth for the whole app).
    private val repo = CardRepository.get(getApplication())

    // The player id we were navigated with. If for some reason it is missing we fall back to -1,
    // which will simply never match any real player -> the card stays null and the screen shows the
    // empty/loading state instead of crashing.
    private val playerId: Int = savedState.get<Int>(ARG_PLAYER_ID) ?: -1

    /**
     * The one card this screen is about, or null while it is still loading / if the id is unknown.
     * We turn the repository Flow into a StateFlow so the Composable can collect it with
     * collectAsState() and so the value survives recomposition.
     */
    val card: StateFlow<AlbumCard?> = repo
        .observePlayerDetail(playerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Flips the favorite flag for the current player. We read the current state from the latest
     * emitted card; if there is no card yet we simply do nothing.
     */
    fun toggleFavorite() {
        val current = card.value ?: return
        viewModelScope.launch {
            repo.setFavorite(current.playerId, !current.isFavorite)
        }
    }

    companion object {
        // Must match Routes.ARG_PLAYER_ID and the NavType.IntType argument name in AppNav.
        const val ARG_PLAYER_ID = "playerId"
    }
}
