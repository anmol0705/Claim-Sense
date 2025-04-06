package android.saswat.claimsense.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.saswat.claimsense.R

@Composable
fun AnimatedBarChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00E5FF), // Cyan color from image
    backgroundColor: Color = Color.Transparent, // Now transparent to match card background
    showLabels: Boolean = true,
    labels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S", "st")
) {
    val barWidth = 36.dp 
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

    val montserratFontFamily = FontFamily(
        Font(R.font.montserratregular, FontWeight.Normal),
        Font(R.font.montserratsemibold, FontWeight.SemiBold),
        Font(R.font.montserratextrabold, FontWeight.ExtraBold),
        Font(R.font.montserratlightitalic, FontWeight.Light) 
    )

    Box(modifier = modifier
        .background(backgroundColor)
        .padding(top = 4.dp, bottom = if (showLabels) 16.dp else 0.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showLabels) 28.dp else 0.dp, top = 4.dp)
        ) {
            val availableWidth = size.width
            val availableHeight = size.height
            val barWidthPx = with(density) { barWidth.toPx() }
            val totalBars = data.size
            val spacing = (availableWidth - (barWidthPx * totalBars)) / (totalBars + 1)

            // Draw background vertical grid lines (darker blue)
            val bgLineColor = Color(0xFF172333)
            val bgLineWidth = with(density) { 1.dp.toPx() }

            val horizontalLinesCount = 4
            val horizontalSpacing = availableHeight / horizontalLinesCount
            for (i in 1 until horizontalLinesCount) {
                val y = i * horizontalSpacing
                drawLine(
                    color = bgLineColor.copy(alpha = 0.3f), // More transparent
                    start = Offset(0f, y),
                    end = Offset(availableWidth, y),
                    strokeWidth = bgLineWidth,
                    cap = StrokeCap.Round
                )
            }

            // Then draw vertical lines
            for (i in 0 until totalBars) {
                val x = spacing + (i * (barWidthPx + spacing)) + barWidthPx / 2
                drawLine(
                    color = bgLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, availableHeight),
                    strokeWidth = bgLineWidth,
                    cap = StrokeCap.Round
                )
            }

            // Draw bars
            animatedValues.forEachIndexed { index, value ->
                val barHeight = value * availableHeight
                val x = spacing + (index * (barWidthPx + spacing))

                // Draw rounded bar with more pronounced corners
                val cornerRadius = with(density) { 18.dp.toPx() }
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, availableHeight - barHeight),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
        
        // Add labels below the chart
        if (showLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal, 
                        fontFamily = montserratFontFamily,
                        color = Color(0xFF8A8D93),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}