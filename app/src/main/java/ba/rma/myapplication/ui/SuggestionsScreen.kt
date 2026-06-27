package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.vm.SuggestionsViewModel

/**
 * FEATURE A — SMART SUGGESTIONS screen.
 *
 * Shows the result of the local analysis: overall progress, the chance the next card is new, the
 * advice lines, per-team progress bars, and the duplicates the user can trade. The heavy lifting is
 * done by the algorithm (SuggestionEngine) via the ViewModel; this screen only draws the result.
 */
@Composable
fun SuggestionsScreen(viewModel: SuggestionsViewModel = viewModel()) {

    // The suggestions are computed reactively in the ViewModel from the repository streams, so there
    // is nothing to trigger here — we just read the latest value and draw it. While the catalog is
    // still loading (or the collection is empty) this is null.
    val data by viewModel.suggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen title.
        Text(
            text = "Pametni prijedlozi",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Show a placeholder while we have no data yet, otherwise draw the full content.
        if (data == null) {
            Text(text = "Računam prijedloge... (otvori paket ako je kolekcija prazna)")
        } else {
            SuggestionsContent(data!!)
        }
    }
}

/**
 * Draws the actual suggestion content in a scrolling list. Split into its own composable so the
 * code above stays focused on the loading/error/empty decision.
 */
@Composable
private fun SuggestionsContent(data: SuggestionsUi) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Overall album completion as a progress bar.
        item {
            ProgressStat(
                label = "Ukupno sakupljeno",
                owned = data.ownedDistinct,
                total = data.totalPlayers
            )
        }

        // The dynamic "is it worth opening more packs" probability.
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sansa da je sljedeca slicica nova: " + data.worthItProbabilityPercent + "%",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // The advice lines from the algorithm.
        item {
            Text(text = "Savjeti", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
        }
        items(data.adviceLines) { line ->
            Text(
                text = "- " + line,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Per-team progress bars (also our simple data visualization).
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Napredak po timovima", style = MaterialTheme.typography.titleMedium)
        }
        items(data.teamProgress) { group ->
            ProgressStat(
                label = group.name,
                owned = group.owned,
                total = group.total
            )
        }

        // Per-position progress bars (the other "category" the algorithm tracks).
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Napredak po pozicijama", style = MaterialTheme.typography.titleMedium)
        }
        items(data.positionProgress) { group ->
            ProgressStat(
                label = group.name,
                owned = group.owned,
                total = group.total
            )
        }

        // Duplicates the user can trade (only shown if there are any).
        if (data.duplicates.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Duplikati (za mijenjanje)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(data.duplicates) { dup ->
                Text(
                    text = "- " + dup.playerName + " x" + dup.count,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
