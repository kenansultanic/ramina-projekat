package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.ui.charts.BarChart
import ba.rma.myapplication.ui.charts.PieChart
import ba.rma.myapplication.vm.StatsViewModel

/**
 * STATISTICS / DATA VISUALIZATION screen.
 *
 * Shows the collection at a glance: overall progress, a pie chart of collected-vs-missing, a per-team
 * bar chart, and a few extra numbers (golden cards, duplicates). The whole thing is wrapped in a
 * pull-to-refresh so the user can re-download the catalog by swiping down.
 *
 * All the calculations live in [StatsViewModel]; this screen only draws what it is given and handles
 * the loading / empty / error states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {

    // Read the ViewModel's Compose state. These are mutableStateOf values, so the screen recomposes
    // automatically whenever the numbers change (e.g. after opening a pack).
    val data = viewModel.data
    val loading = viewModel.loading
    val error = viewModel.errorMessage

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        // Decide what to show. We treat "no data yet" specially so the user always sees something.
        when {
            // First load in progress and nothing computed yet -> spinner.
            data == null && loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // No data and a refresh failed -> error with a retry button.
            data == null && error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text(text = "Pokušaj ponovo")
                    }
                }
            }

            // Catalog exists but the user hasn't collected anything yet.
            data != null && data.totalPlayers > 0 && data.ownedDistinct == 0 -> {
                EmptyStats()
            }

            // The catalog itself is empty (nothing downloaded yet) and no error.
            data == null || data.totalPlayers == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema podataka. Povuci nadolje za osvježavanje.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // The normal case: we have a populated catalog and at least one collected sticker.
            else -> {
                StatsContent(data)
            }
        }
    }
}

/**
 * The actual statistics content, laid out in a vertical scroll. Pulled into its own composable so the
 * state-deciding code above stays readable. A plain Column + verticalScroll is fine here (the content
 * is short and bounded), and it plays nicely inside PullToRefreshBox.
 */
@Composable
private fun StatsContent(data: StatsUi) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Header ---
        Text(
            text = "Statistika kolekcije",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        // --- Overall progress bar (owned vs total) ---
        ProgressStat(
            label = "Ukupno sakupljeno",
            owned = data.ownedDistinct,
            total = data.totalPlayers
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Pie chart: collected vs missing ---
        Text(
            text = "Sakupljene i nedostajuće",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PieChart(owned = data.ownedDistinct, missing = data.missing)
        }
        Spacer(modifier = Modifier.height(20.dp))

        // --- A few extra numbers: golden cards and duplicates ---
        StatRow(label = "Zlatne sličice", value = data.goldenCount.toString())
        StatRow(label = "Duplikati (za mijenjanje)", value = data.totalDuplicates.toString())
        StatRow(label = "Nedostaje", value = data.missing.toString())
        Spacer(modifier = Modifier.height(20.dp))

        // --- Per-team completion bar chart ---
        Text(
            text = "Napredak po timovima",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (data.teamStats.isEmpty()) {
            Text(
                text = "Nema timova za prikaz.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            BarChart(teams = data.teamStats, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** A single "label .... value" row for the extra numbers (golden, duplicates, missing). */
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Shown when the catalog is loaded but the user has not collected a single sticker yet. We still let
 * them pull to refresh (the parent PullToRefreshBox handles that), so we just fill the area with a
 * friendly message.
 */
@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Još nisi sakupio nijednu sličicu.\nOtvori paket pa se vrati ovdje da vidiš statistiku!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
