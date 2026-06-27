package ba.rma.myapplication.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ba.rma.myapplication.ui.TeamStatUi

/**
 * A horizontal BAR CHART of per-team completion, drawn with the Compose Canvas API (no external
 * library). Each team is one row: the team name, a bar whose filled part shows the completion
 * fraction (owned / total), and the "owned/total" numbers.
 */
@Composable
fun BarChart(teams: List<TeamStatUi>, modifier: Modifier = Modifier) {
    // Read theme colors once (cannot read MaterialTheme inside the Canvas block).
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val barColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        for (team in teams) {
            // Completion as a 0.0 .. 1.0 fraction (guard against dividing by zero).
            val fraction = if (team.total > 0) {
                team.owned.toFloat() / team.total.toFloat()
            } else {
                0f
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team name on the left (fixed width so all bars start at the same place).
                Text(
                    text = team.team,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.width(110.dp)
                )

                // The bar itself, drawn with Canvas: a grey track plus a colored filled part.
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    // Background track (the full bar).
                    drawRect(color = trackColor, size = size)
                    // Filled part proportional to completion.
                    drawRect(
                        color = barColor,
                        size = Size(width = size.width * fraction, height = size.height)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // The numbers on the right.
                Text(
                    text = team.owned.toString() + "/" + team.total.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}
