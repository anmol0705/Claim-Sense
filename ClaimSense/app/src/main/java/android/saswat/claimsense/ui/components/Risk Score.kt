package android.saswat.claimsense.ui.components

import android.saswat.claimsense.viewmodel.RiskViewModel
import android.saswat.claimsense.viewmodel.RiskViewState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun RiskScoreScreen(
    riskViewModel: RiskViewModel
) {
    val riskViewState by riskViewModel.riskViewState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                when (riskViewState) {
                    is RiskViewState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    is RiskViewState.Success -> {
                        val data = riskViewState as RiskViewState.Success
                        val summary = data.summary
                        val riskHistory = data.riskHistory
                        val dailyAverages = data.dailyAverages

                        // Risk Score Overview Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Risk Score",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when {
                                                    summary.averageRisk < 30 -> Color(0xFF4CAF50)
                                                    summary.averageRisk < 70 -> Color(0xFFFF9800)
                                                    else -> Color(0xFFF44336)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                summary.averageRisk < 30 -> "Low"
                                                summary.averageRisk < 70 -> "Medium"
                                                else -> "High"
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Risk Score Display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        RiskScoreGauge(
                                            score = summary.averageRisk,
                                            maxScore = 100f,
                                            size = 180.dp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "${summary.averageRisk.toInt()}",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Text(
                                            text = if (summary.riskTrend > 0)
                                                "↑ Increasing by ${String.format(Locale.US,"%.1f", summary.riskTrend)}"
                                            else
                                                "↓ Decreasing by ${String.format(Locale.US,"%.1f", -summary.riskTrend)}",
                                            fontSize = 14.sp,
                                            color = if (summary.riskTrend > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xFF333333),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // Risk Stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    RiskStatItem(
                                        label = "Maximum",
                                        value = summary.maxRisk.toInt().toString()
                                    )

                                    VerticalDivider()

                                    RiskStatItem(
                                        label = "Records",
                                        value = summary.recordCount.toString()
                                    )

                                    VerticalDivider()

                                    RiskStatItem(
                                        label = "Last Update",
                                        value = summary.lastRecordDate.split(" ")[0]
                                    )
                                }
                            }
                        }

                        // Risk History Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    "Risk History",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Simple chart visualization
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(
                                            color = Color(0xFF2A2A2A),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    val sortedEntries = dailyAverages.entries.sortedBy { it.key }.takeLast(7)
                                    if (sortedEntries.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            sortedEntries.forEach { entry ->
                                                val date = entry.key
                                                val value = entry.value
                                                val normalizedHeight = value.coerceIn(0f, 100f) / 100f
                                                val animatedHeight by animateFloatAsState(
                                                    targetValue = normalizedHeight,
                                                    label = "chart_bar"
                                                )

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Bottom
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(30.dp)
                                                            .fillMaxHeight(animatedHeight)
                                                            .background(
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        Color(0xFF00E5FF),
                                                                        Color(0xFF00B3CC)
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                            )
                                                    )

                                                    Text(
                                                        text = date.split("-").last(),  // Show day only
                                                        fontSize = 12.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No history data available",
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                // Filter options
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { riskViewModel.fetchRiskData(RiskViewModel.TimeRange.WEEK) },
                                        label = { Text("Week") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00B3CC),
                                            selectedLabelColor = Color.Black
                                        )
                                    )

                                    FilterChip(
                                        selected = false,
                                        onClick = { riskViewModel.fetchRiskData(RiskViewModel.TimeRange.MONTH) },
                                        label = { Text("Month") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00B3CC),
                                            selectedLabelColor = Color.Black
                                        )
                                    )

                                    FilterChip(
                                        selected = false,
                                        onClick = { riskViewModel.fetchRiskData(RiskViewModel.TimeRange.YEAR) },
                                        label = { Text("Year") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00B3CC),
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        // Recent Risk Records List
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    "Recent Records",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Show top 5 most recent records
                                riskHistory.take(5).forEach { record ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = record.formattedDate,
                                            color = Color.LightGray,
                                            fontSize = 14.sp
                                        )

                                        val riskValue = record.avgRisk ?: record.risk
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = when {
                                                        riskValue < 30 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                        riskValue < 70 -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                                        else -> Color(0xFFF44336).copy(alpha = 0.2f)
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = riskValue.toInt().toString(),
                                                color = when {
                                                    riskValue < 30 -> Color(0xFF4CAF50)
                                                    riskValue < 70 -> Color(0xFFFF9800)
                                                    else -> Color(0xFFF44336)
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    if (riskHistory.indexOf(record) < 4) {
                                        HorizontalDivider(
                                            color = Color(0xFF333333),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is RiskViewState.Error -> {
                        val errorData = riskViewState as RiskViewState.Error

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(bottom = 16.dp)
                                )

                                Text(
                                    "Unable to load risk data",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    errorData.message,
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { riskViewModel.fetchRiskData() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Try Again",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskScoreGauge(
    score: Float,
    maxScore: Float,
    size: Dp
) {
    val sweepAngle = 240f * (score / maxScore).coerceIn(0f, 1f)
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngle,
        label = "gauge_animation"
    )

    Box(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background arc
            drawArc(
                color = Color(0xFF333333),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )

            // Draw colored progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),  // Green
                        Color(0xFFFFEB3B),  // Yellow
                        Color(0xFFFF9800),  // Orange
                        Color(0xFFF44336)   // Red
                    )
                ),
                startAngle = 150f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun RiskStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .background(Color(0xFF333333))
    )
}