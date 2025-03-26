package android.saswat.claimsense.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedBarChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    val barWidth = 24.dp
    val barColor = Color.White
    val density = LocalDensity.current

    val animatedValues = data.map { targetValue ->
        val animatedValue by animateFloatAsState(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            label = "BarAnimation"
        )
        animatedValue
    }

    Canvas(
        modifier = modifier
    ) {
        val availableWidth = size.width
        val availableHeight = size.height
        val barWidthPx = with(density) { barWidth.toPx() }
        val totalBars = data.size
        val spacing = (availableWidth - (barWidthPx * totalBars)) / (totalBars + 1)

        animatedValues.forEachIndexed { index, value ->
            val barHeight = value * availableHeight
            val x = spacing + (index * (barWidthPx + spacing))
            
            // Draw bar
            drawRect(
                color = barColor,
                topLeft = Offset(x, availableHeight - barHeight),
                size = Size(barWidthPx, barHeight)
            )
        }
    }
}