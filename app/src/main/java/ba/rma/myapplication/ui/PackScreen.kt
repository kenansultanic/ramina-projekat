package ba.rma.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.sensors.OpenPackFallback
import ba.rma.myapplication.sensors.ShakeToOpen
import ba.rma.myapplication.vm.PackViewModel

/**
 * FEATURE B — SHAKE-TO-OPEN screen.
 *
 * Shaking the phone opens a pack of 5 random cards, which are then revealed and saved into the
 * collection. There is also a button that does the exact same thing (so the feature works on an
 * emulator that has no accelerometer).
 *
 * This screen does NOT touch the sensor itself: [ShakeToOpen] (in the sensors package) registers
 * and unregisters the accelerometer for the screen's lifetime and just calls us back on a shake.
 */
@Composable
fun PackScreen(viewModel: PackViewModel = viewModel()) {

    // Connect the shake gesture to "open a pack". The ViewModel ignores the call if a pack is
    // already being opened, so a flurry of shakes still opens only one pack.
    ShakeToOpen(onShake = { viewModel.openPack() })

    // Read the current state into local values. Reading them here lets Compose know this screen
    // depends on them, so it redraws when they change.
    val loading = viewModel.isLoading
    val error = viewModel.errorMessage
    val cards = viewModel.revealedCards

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Protresi telefon da otvoris paket!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        // The manual fallback button. alwaysShow = true so it is available everywhere (emulators
        // have no accelerometer, and it is handy on real devices too). The button disables itself
        // while a pack is being opened.
        OpenPackFallback(
            onOpenClick = { viewModel.openPack() },
            isOpening = loading,
            alwaysShow = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show exactly one of: loading, error, "nothing yet", or the opened pack.
        if (loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Otvaram paket...")
        } else if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.clearError() }) {
                Text(text = "U redu")
            }
        } else if (cards.isEmpty()) {
            Text(text = "Jos nisi otvorio nijedan paket.")
        } else {
            Text(
                text = "Tvoj paket:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Two columns of cards. weight(1f) lets the grid fill the leftover vertical space and
            // scroll on its own. We key items by their position in the pack (a pack can contain the
            // same player twice, so the player id alone would not be unique).
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                itemsIndexed(cards, key = { index, _ -> index }) { _, card ->
                    // Each card fades and scales in when it first appears, for a nice reveal.
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        shown = true
                    }
                    AnimatedVisibility(
                        visible = shown,
                        enter = fadeIn() + scaleIn()
                    ) {
                        StickerCard(
                            card = card,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
