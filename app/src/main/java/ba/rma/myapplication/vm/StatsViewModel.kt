package ba.rma.myapplication.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.AlbumCard
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.ui.StatsUi
import ba.rma.myapplication.ui.TeamStatUi
import kotlinx.coroutines.launch

/**
 * ViewModel for the STATISTICS (data visualization) screen.
 *
 * It listens to the whole album from the repository and turns it into a compact [StatsUi] object
 * (overall counts + per-team breakdown) that the screen draws as a pie chart, a bar chart and a few
 * progress rows. The repository is the single source of truth, so whenever the user opens a pack the
 * numbers here update automatically.
 *
 * State is exposed as Compose [mutableStateOf] with a private setter, so the screen can read it but
 * only the ViewModel can change it.
 */
class StatsViewModel(app: Application) : AndroidViewModel(app) {

    // The shared repository singleton (same instance every other ViewModel uses).
    private val repo = CardRepository.get(app)

    /** The computed statistics, or null until the first emission arrives. */
    var data: StatsUi? by mutableStateOf(null)
        private set

    /** True while a manual refresh (network catalog download) is in progress. */
    var loading: Boolean by mutableStateOf(false)
        private set

    /** A Bosnian error message to show, or null when there is nothing to report. */
    var errorMessage: String? by mutableStateOf(null)
        private set

    init {
        // Start observing the album. Every time the catalog or the owned table changes, the repo
        // emits a fresh list and we recompute the stats. This keeps the screen always up to date.
        viewModelScope.launch {
            repo.observeAlbum().collect { cards ->
                data = computeStats(cards)
            }
        }
    }

    /**
     * Manually re-downloads the catalog from the network (e.g. pull-to-refresh). This is best-effort:
     * the repository never throws and never clears the cache. We only surface an error if the refresh
     * failed AND we still have nothing to show, so an offline user with cached data is not nagged.
     */
    fun refresh() {
        viewModelScope.launch {
            loading = true
            val ok = repo.refreshCatalog()
            if (!ok && data == null) {
                errorMessage = "Nije moguće učitati podatke. Provjeri internet konekciju."
            }
            loading = false
        }
    }

    /** Dismisses the current error message. */
    fun clearError() {
        errorMessage = null
    }

    /**
     * Turns the raw album list into the small [StatsUi] the screen needs.
     *
     * - ownedDistinct: how many distinct players the user owns at least one of.
     * - totalPlayers: how many players exist in the catalog.
     * - missing: how many are still not owned.
     * - goldenCount: how many owned cards are golden/rare.
     * - totalDuplicates: total spare copies (every copy beyond the first counts as a duplicate).
     * - teamStats: one entry per national team, with owned/total, sorted by team name.
     */
    private fun computeStats(cards: List<AlbumCard>): StatsUi {
        val totalPlayers = cards.size
        val ownedDistinct = cards.count { it.isOwned }
        val missing = totalPlayers - ownedDistinct
        val goldenCount = cards.count { it.isGolden && it.isOwned }

        // Every copy after the first one is a duplicate (a card you could trade away).
        var totalDuplicates = 0
        for (card in cards) {
            if (card.ownedCount > 1) {
                totalDuplicates += card.ownedCount - 1
            }
        }

        // Group the cards by national team and count owned/total within each group.
        val teamStats = ArrayList<TeamStatUi>()
        val grouped = cards.groupBy { it.reprezentacija }
        for ((team, teamCards) in grouped) {
            teamStats.add(
                TeamStatUi(
                    team = team,
                    owned = teamCards.count { it.isOwned },
                    total = teamCards.size
                )
            )
        }
        // Sort alphabetically so the bar chart has a stable, readable order.
        teamStats.sortBy { it.team }

        return StatsUi(
            ownedDistinct = ownedDistinct,
            totalPlayers = totalPlayers,
            missing = missing,
            goldenCount = goldenCount,
            totalDuplicates = totalDuplicates,
            teamStats = teamStats
        )
    }
}
