package ba.rma.myapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.ui.DuplicateUi
import ba.rma.myapplication.ui.GroupProgressUi
import ba.rma.myapplication.ui.SuggestionsUi
import ba.rma.myapplication.ui.positionLabelBs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * FEATURE A — SMART SUGGESTIONS (the ViewModel).
 *
 * This ViewModel is intentionally "dumb": it does no networking or storage of its own. It just
 * listens to the repository's two reactive streams (the full catalog of players, and what the user
 * currently owns), feeds them into the pure [SuggestionEngine], and converts the resulting
 * [AlbumAnalysis] into a UI-friendly [SuggestionsUi].
 *
 * Because everything is reactive, the suggestions update automatically whenever the user opens a
 * pack or the catalog is refreshed — there is no manual "refresh" button needed.
 */
class SuggestionsViewModel(app: Application) : AndroidViewModel(app) {

    // Shared singleton repository (offline-first single source of truth).
    private val repo = CardRepository.get(getApplication())

    /**
     * The suggestions to show on screen, or `null` while we don't have data yet.
     *
     * We combine the catalog stream and the owned stream. If the catalog is still empty (e.g. the
     * very first launch before it has loaded), we emit `null` so the screen can show a friendly
     * "computing..." placeholder. Otherwise we run the analysis and map it to the UI model.
     */
    val suggestions: StateFlow<SuggestionsUi?> =
        combine(repo.observeCatalogPlayers(), repo.observeOwned()) { catalog, owned ->
            if (catalog.isEmpty()) {
                // No catalog yet -> nothing meaningful to suggest. Show the placeholder.
                null
            } else {
                // Run the pure algorithm, then convert its output into the UI shape.
                mapToUi(SuggestionEngine.analyze(catalog, owned))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Converts the raw [AlbumAnalysis] (English-ish, math-focused) into the [SuggestionsUi] the
     * screen draws. The main translation work here is making the position names human-friendly and
     * Bosnian, and turning "spare copies" into a "total copies" count for display.
     */
    private fun mapToUi(analysis: AlbumAnalysis): SuggestionsUi {
        return SuggestionsUi(
            ownedDistinct = analysis.ownedUnique,
            totalPlayers = analysis.totalPlayers,
            // Per-team progress: team names are already display-ready, pass them straight through.
            teamProgress = analysis.perTeam.map { group ->
                GroupProgressUi(
                    name = group.name,
                    owned = group.owned,
                    total = group.total
                )
            },
            // Per-position progress: translate the API position name (e.g. "FORWARD") into a short
            // Bosnian label (e.g. "Napad") so the UI reads naturally.
            positionProgress = analysis.perPosition.map { group ->
                GroupProgressUi(
                    name = positionLabelBs(group.name),
                    owned = group.owned,
                    total = group.total
                )
            },
            worthItProbabilityPercent = analysis.newCardChancePercent,
            // DuplicateInfo.spareCopies counts only the EXTRA copies; for display we want the TOTAL
            // number of copies the user holds, so we add back the one kept for the album (+1).
            duplicates = analysis.duplicates.map { dup ->
                DuplicateUi(
                    playerName = dup.name,
                    count = dup.spareCopies + 1
                )
            },
            adviceLines = analysis.suggestions
        )
    }
}
