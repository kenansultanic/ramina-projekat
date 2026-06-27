package ba.rma.myapplication.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.data.Player
import ba.rma.myapplication.ui.CardUi
import kotlinx.coroutines.launch

/**
 * ViewModel for FEATURE B — the SHAKE-TO-OPEN ("Paketi") screen.
 *
 * It owns the logic for opening a pack of cards and revealing them. The screen only reads the
 * state below and calls [openPack] / [clearError] — all the work lives here.
 *
 * State is exposed as Compose [mutableStateOf] with a private setter, so only this ViewModel can
 * change it while the screen can read it directly (Compose tracks the reads and redraws on change).
 */
class PackViewModel(app: Application) : AndroidViewModel(app) {

    // The one shared repository (single source of truth). Every ViewModel gets the same instance.
    private val repo = CardRepository.get(app)

    // True while a pack is being opened (network call in flight). Used to show a spinner and to
    // ignore extra shakes/clicks.
    var isLoading: Boolean by mutableStateOf(false)
        private set

    // A user-facing error message (Bosnian) or null when there is nothing to show.
    var errorMessage: String? by mutableStateOf(null)
        private set

    // The cards from the most recently opened pack, ready for the reveal grid. Empty before the
    // first pack is opened.
    var revealedCards: List<CardUi> by mutableStateOf(emptyList())
        private set

    /**
     * Opens a single pack of [PACK_SIZE] cards.
     *
     * The guard is placed FIRST, INSIDE the coroutine. Because [viewModelScope] runs on the main
     * thread by default, the `isLoading` check-and-set happens on the main thread, so a quick burst
     * of shakes can never start two overlapping pack openings (no race on the flag).
     */
    fun openPack() {
        viewModelScope.launch {
            // Guard: if a pack is already opening, do nothing. Runs on the main thread.
            if (isLoading) return@launch

            isLoading = true
            errorMessage = null
            try {
                // Ask the server for PACK_SIZE random players (duplicates are possible).
                val players = repo.openPackFromServer(PACK_SIZE)

                // Convert them to UI cards so the screen can draw them right away.
                revealedCards = players.map { toCardUi(it) }

                // Persist each opened card into the collection (increments count if already owned).
                for (player in players) {
                    repo.addOpenedCard(player)
                }
            } catch (e: Exception) {
                // Network or parsing problem: show a friendly Bosnian message and keep the old cards.
                errorMessage = "Greska pri otvaranju paketa. Provjeri internet konekciju."
            } finally {
                isLoading = false
            }
        }
    }

    /** Dismisses the current error message (called when the user taps the "U redu" button). */
    fun clearError() {
        errorMessage = null
    }

    /**
     * Maps a freshly pulled [Player] into a [CardUi]. A pack card is always owned (ownedCount = 1)
     * and may be golden (the catalog never reports golden, only packs do via `zlatna`).
     */
    private fun toCardUi(player: Player): CardUi {
        return CardUi(
            playerId = player.id,
            firstName = player.ime,
            lastName = player.prezime,
            jerseyNumber = player.brojDresa,
            team = player.reprezentacija,
            position = player.pozicija,
            imagePath = player.slicicaLokacija,
            isGolden = player.zlatna ?: false,
            ownedCount = 1
        )
    }

    companion object {
        // How many cards are in one pack.
        private const val PACK_SIZE = 5
    }
}
