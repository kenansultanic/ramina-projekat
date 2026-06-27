package ba.rma.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.vm.FavoritesViewModel

/**
 * Favorites screen: shows every card the user marked as a favorite.
 *
 * The cards are drawn in a responsive grid that fits as many columns as the screen width allows.
 * Each card is clickable and opens the player's detail screen. When there are no favorites yet a
 * clear, friendly empty state is shown instead of a blank screen.
 */
@Composable
fun FavoritesScreen(
    onOpenDetail: (Int) -> Unit,
    viewModel: FavoritesViewModel = viewModel()
) {
    // The list of favorite cards (already sorted by jersey number in the ViewModel).
    val favorites by viewModel.favorites.collectAsState()

    if (favorites.isEmpty()) {
        // ----- EMPTY STATE -----
        // No favorites yet — explain how to add some.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Nema omiljenih sličica",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Otvorite detalje neke sličice i dodirnite srce da je dodate među omiljene.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // ----- CONTENT -----
        // Responsive grid: as many ~160dp columns as fit the screen width.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = favorites,
                key = { it.playerId }
            ) { card ->
                // Convert the domain card into the UI model StickerCard knows how to draw,
                // and make the whole card open the detail screen on tap.
                StickerCard(
                    card = card.toCardUi(),
                    modifier = Modifier.clickable { onOpenDetail(card.playerId) }
                )
            }
        }
    }
}
