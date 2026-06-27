package ba.rma.myapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.AlbumCard
import ba.rma.myapplication.data.CardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Favorites screen.
 *
 * It simply mirrors the repository's stream of favorite cards, sorted by jersey number so the list
 * always shows in a predictable order. The screen only reads [favorites] and draws it — no logic
 * lives in the UI.
 */
class FavoritesViewModel(app: Application) : AndroidViewModel(app) {

    // Shared singleton repository (offline-first single source of truth).
    private val repo = CardRepository.get(getApplication())

    /**
     * The user's favorite cards, sorted ascending by [AlbumCard.brojDresa] (jersey number).
     * Exposed as a StateFlow so the screen can collectAsState() and re-draw on every change.
     */
    val favorites: StateFlow<List<AlbumCard>> =
        repo.observeFavorites()
            .map { cards -> cards.sortedBy { it.brojDresa } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
