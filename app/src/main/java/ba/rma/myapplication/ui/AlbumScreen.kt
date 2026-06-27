package ba.rma.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.data.SortOrder
import ba.rma.myapplication.vm.AlbumViewModel

/**
 * The Album screen: the heart of the app. It shows every player as a [StickerCard] in a responsive
 * grid, with:
 *  - a search box (filter by name),
 *  - status filter chips (Sve / Skupljene / Nedostaju / Favoriti),
 *  - a team dropdown and a sort dropdown,
 *  - pull-to-refresh to re-download the catalog,
 *  - and graceful empty/error states.
 *
 * All the logic lives in [AlbumViewModel]; this composable just reads [AlbumViewModel.uiState] and
 * draws it, calling the ViewModel's functions when the user interacts.
 *
 * NOTE: we use `collectAsState()` (NOT collectAsStateWithLifecycle, which isn't on the classpath).
 * @OptIn is needed because PullToRefreshBox and FilterChip are experimental Material 3 APIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onOpenDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = viewModel()
) {
    // One immutable snapshot of everything the screen needs.
    val state by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {

        // ---------------- Search box ----------------
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchChange(it) },
            label = { Text("Pretraži igrače") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // ---------------- Status filter chips ----------------
        // A simple Row of chips. With only four short options this fits comfortably on phones.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // StatusFilter is defined in AlbumFilters.kt (same ui package).
            for (status in StatusFilter.entries) {
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.onStatusChange(status) },
                    label = { Text(status.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------------- Team + Sort dropdowns ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Team dropdown.
            TeamDropdown(
                teams = state.teams,
                selectedTeam = state.teamFilter,
                onTeamSelected = { viewModel.onTeamChange(it) },
                modifier = Modifier.weight(1f)
            )
            // Sort dropdown.
            SortDropdown(
                selectedSort = state.sortOrder,
                onSortSelected = { viewModel.onSortChange(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------------- Error banner (if any) ----------------
        // Refresh failures show a dismissible message but DON'T replace the cached grid below.
        if (state.errorMessage != null) {
            ErrorBanner(
                message = state.errorMessage!!,
                onDismiss = { viewModel.clearError() },
                onRetry = { viewModel.refresh() }
            )
        }

        // ---------------- The grid, wrapped in pull-to-refresh ----------------
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isEmpty) {
                // Empty state: nothing matches the current filters (or the album isn't loaded yet).
                EmptyState()
            } else {
                LazyVerticalGrid(
                    // Adaptive cells make the grid responsive: more columns on wider screens.
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // playerId is unique within the album list, so it's a safe, stable item key.
                    items(
                        items = state.cards,
                        key = { card -> card.playerId }
                    ) { albumCard ->
                        // Tapping a card opens that player's detail screen.
                        StickerCard(
                            card = albumCard.toCardUi(),
                            modifier = Modifier.clickable { onOpenDetail(albumCard.playerId) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dropdown that lets the user pick a team to filter by (or "Sve reprezentacije" for no filter).
 * Built as an OutlinedButton that opens a DropdownMenu — simple and works without extra dependencies.
 */
@Composable
private fun TeamDropdown(
    teams: List<String>,
    selectedTeam: String,
    onTeamSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            // maxLines via single Text; long team names just truncate gracefully.
            Text(text = selectedTeam, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (team in teams) {
                DropdownMenuItem(
                    text = { Text(team) },
                    onClick = {
                        onTeamSelected(team)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * A dropdown for choosing the sort order. The button shows the current order's Bosnian label
 * (via [sortLabel]) and the menu lists all three options.
 */
@Composable
private fun SortDropdown(
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = sortLabel(selectedSort), maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // SortOrder has three values; list them all with their Bosnian labels.
            for (order in SortOrder.entries) {
                DropdownMenuItem(
                    text = { Text(sortLabel(order)) },
                    onClick = {
                        onSortSelected(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** A dismissible error message shown above the grid when a refresh fails. */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onRetry) { Text("Pokušaj") }
        Spacer(modifier = Modifier.width(4.dp))
        Button(onClick = onDismiss) { Text("U redu") }
    }
}

/** Centered message shown when no cards match the current filters. */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nema sličica za prikaz.\nPromijenite filtere ili povucite da osvježite.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}
