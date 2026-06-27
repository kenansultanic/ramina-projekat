package ba.rma.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * A reusable card that shows one player's sticker: the picture, the name, jersey number, team and
 * position. It adapts to the card's state:
 *  - NOT owned (ownedCount == 0): drawn faded with a small "Nedostaje" (missing) label.
 *  - DUPLICATE (ownedCount > 1): shows an "xN" badge so the user sees how many spares they have.
 *  - GOLDEN: gold border + "ZLATNA" badge.
 *  - FAVORITE: a small heart next to the name.
 *
 * The sticker images are large (~2.7 MB PNGs). We do NOT decode them ourselves: Coil's AsyncImage
 * downloads on a background thread, shrinks to the drawn size, and caches the result.
 */
@Composable
fun StickerCard(card: CardUi, modifier: Modifier = Modifier) {
    val goldColor = Color(0xFFFFC107) // amber/gold
    val cardShape = RoundedCornerShape(12.dp)

    // Missing stickers are shown faded so the album clearly shows what is still to collect.
    val contentAlpha = if (card.ownedCount == 0) 0.4f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        shape = cardShape,
        // Only golden cards get a border; normal cards get null (no border).
        border = if (card.isGolden) BorderStroke(2.dp, goldColor) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // The picture area, with a fixed shape so every card looks the same while loading.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(API_BASE_URL + card.imagePath) // e.g. http://...:3300/api/slicica/3
                        .crossfade(true)
                        .build(),
                    contentDescription = card.firstName + " " + card.lastName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // "xN" duplicate badge in the top-left corner.
                if (card.ownedCount > 1) {
                    Text(
                        text = "x" + card.ownedCount,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Golden badge in the top-right corner.
                if (card.isGolden) {
                    Text(
                        text = "ZLATNA",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(goldColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // "Nedostaje" (missing) label centered over faded missing cards.
                if (card.ownedCount == 0) {
                    Text(
                        text = "Nedostaje",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0xAA000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Name + (optional) favorite heart on the same row.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.firstName + " " + card.lastName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (card.isFavorite) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "♥", // heart character
                        color = Color(0xFFE53935), // red
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Jersey number + team.
            Text(
                text = "#" + card.jerseyNumber + "  " + card.team,
                style = MaterialTheme.typography.bodySmall
            )
            // Position, shown in Bosnian.
            Text(
                text = positionLabelBs(card.position),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
