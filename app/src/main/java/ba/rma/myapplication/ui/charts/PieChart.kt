package ba.rma.myapplication.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A simple PIE CHART of collected-vs-missing stickers, drawn by hand with the Compose Canvas API
 * (no external chart library). One slice is the owned stickers, the other is the missing ones.
 *
 * How the drawing works: a full circle is 360 degrees. The owned slice gets a share of those 360
 * degrees equal to its share of the total (owned / total). We start at -90 degrees so the chart
 * begins at the top (12 o'clock), which looks the most natural.
 */
@Composable
fun PieChart(owned: Int, missing: Int, modifier: Modifier = Modifier) {
    val total = owned + missing

    // Read theme colors here (we cannot call MaterialTheme inside the Canvas draw block).
    val ownedColor = MaterialTheme.colorScheme.primary
    val missingColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.size(180.dp)) {
            if (total <= 0) {
                // Nothing collected yet: just draw an empty (grey) circle.
                drawArc(
                    color = missingColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true
                )
                return@Canvas
            }

            // Degrees for the owned slice (the rest is the missing slice).
            val ownedSweep = 360f * owned / total

            // Owned slice, starting at the top.
            drawArc(
                color = ownedColor,
                startAngle = -90f,
                sweepAngle = ownedSweep,
                useCenter = true
            )
            // Missing slice fills the remainder of the circle.
            drawArc(
                color = missingColor,
                startAngle = -90f + ownedSweep,
                sweepAngle = 360f - ownedSweep,
                useCenter = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend so the colors are clear.
        LegendRow(color = ownedColor, label = "Skupljene: " + owned)
        Spacer(modifier = Modifier.height(4.dp))
        LegendRow(color = missingColor, label = "Nedostaju: " + missing)
    }
}

/** One legend line: a small colored square + a text label. */
@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
