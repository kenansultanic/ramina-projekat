package ba.rma.myapplication.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import ba.rma.myapplication.data.AlbumCard
import ba.rma.myapplication.vm.DetailViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Detail screen for a single player. Shows a large picture and all of the player's data, lets the
 * user toggle the favorite flag, and share the sticker with a friend via the system share sheet.
 *
 * @param onBack called when the back arrow is tapped.
 * @param viewModel supplies the observed card; the default factory injects the player id from nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    // The current card (null while loading or if the id is unknown).
    val card by viewModel.card.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                // Title shows the player's name once we have it, otherwise a neutral fallback.
                title = { Text(text = card?.fullName ?: "Detalji sličice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad"
                        )
                    }
                },
                actions = {
                    // The favorite + share actions only make sense once a real card is loaded.
                    val currentCard = card
                    if (currentCard != null) {
                        // Favorite toggle: filled heart when favorite, outline when not.
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (currentCard.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = if (currentCard.isFavorite) {
                                    "Ukloni iz favorita"
                                } else {
                                    "Dodaj u favorite"
                                },
                                tint = if (currentCard.isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Share action: opens the Android share sheet with a Bosnian message.
                        IconButton(onClick = { shareCard(context, currentCard) }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Podijeli"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        val currentCard = card
        if (currentCard == null) {
            // ---- Loading / empty state ----
            // We cannot tell "still loading" from "unknown id" here, so we show a friendly spinner
            // plus a hint. In practice the card appears almost immediately for valid ids.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Učitavanje sličice...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // ---- Loaded content ----
            DetailContent(
                card = currentCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * The scrollable body of the detail screen: a big picture followed by all of the player's fields.
 * Kept private since only [DetailScreen] uses it.
 */
@Composable
private fun DetailContent(card: AlbumCard, modifier: Modifier = Modifier) {
    val goldColor = Color(0xFFFFC107) // amber/gold, same as StickerCard

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ---- Large picture ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp)
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(API_BASE_URL + card.slicicaLokacija) // e.g. http://...:3300/api/slicica/3
                    .crossfade(true)
                    .build(),
                contentDescription = card.fullName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Badges, mirroring the small StickerCard so the detail looks consistent.
            if (card.isGolden) {
                Text(
                    text = "ZLATNA",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(goldColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            // If the user does not own this sticker, make that obvious.
            if (!card.isOwned) {
                Text(
                    text = "Nedostaje",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Name (big) ----
        Text(
            text = card.fullName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- All fields, laid out as label/value rows inside a card ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(label = "Broj dresa", value = "#" + card.brojDresa)
                DetailRow(label = "Reprezentacija", value = card.reprezentacija)
                // Position is translated into a short Bosnian label.
                DetailRow(label = "Pozicija", value = positionLabelBs(card.pozicija))
                DetailRow(
                    label = "Zlatna",
                    value = if (card.isGolden) "Da" else "Ne"
                )
                DetailRow(
                    label = "Favorit",
                    value = if (card.isFavorite) "Da" else "Ne"
                )
                // Owned count: how many copies the user has (0 = does not own it).
                DetailRow(
                    label = "Posjedujem",
                    value = if (card.isOwned) {
                        card.ownedCount.toString() + " kom."
                    } else {
                        "Nemam ovu sličicu"
                    }
                )
            }
        }
    }
}

/**
 * A single label/value line used in the detail card. Label on the left, value (bold) on the right.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Builds a friendly Bosnian message about the card and hands it to Android's share sheet so the user
 * can send it via WhatsApp, Viber, SMS, etc. Uses Intent.ACTION_SEND wrapped in a chooser.
 */
private fun shareCard(context: android.content.Context, card: AlbumCard) {
    val text = "Pogledaj moju sličicu: ${card.fullName} (#${card.brojDresa}, ${card.reprezentacija}) " +
        "- pozicija: ${positionLabelBs(card.pozicija)}." +
        (if (card.isGolden) " ✨ Ovo je ZLATNA sličica!" else "") +
        " Skupljam album u Slicice aplikaciji!"

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    // The chooser lets the user pick which app to share through.
    context.startActivity(Intent.createChooser(sendIntent, "Podijeli sličicu"))
}
