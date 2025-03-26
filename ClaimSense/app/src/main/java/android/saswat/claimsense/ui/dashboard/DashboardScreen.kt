package android.saswat.claimsense.ui.dashboard

import android.saswat.claimsense.ui.components.AnimatedBarChart
import android.saswat.claimsense.viewmodel.AuthViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    authViewModel: AuthViewModel
) {
    val userData by authViewModel.userData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1F))
            .padding(16.dp)
    ) {
        // User Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2B2B30)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = userData?.username ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "ID: ${userData?.userId?.take(8) ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // Stats Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Risk Score",
                value = "$45,231.89",
                change = "+20.1%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Interest Due",
                value = "+2350",
                change = "+150.1%",
                modifier = Modifier.weight(1f)
            )
        }

        // Claims and Disputes Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryCard(
                title = "Claims",
                items = listOf(
                    HistoryItem("19-03-25", "Scrutiny"),
                    HistoryItem("19-03-25", "Approved"),
                    HistoryItem("19-03-25", "Denied")
                ),
                modifier = Modifier.weight(1f)
            )
            HistoryCard(
                title = "Disputes",
                items = listOf(
                    HistoryItem("19-03-25", "Scrutiny"),
                    HistoryItem("19-03-25", "Approved"),
                    HistoryItem("19-03-25", "Review")
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Risk Overview Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2B2B30)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Risk Overview",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AnimatedBarChart(
                    data = listOf(0.3f, 0.5f, 0.2f, 0.8f, 0.6f, 0.4f, 0.7f, 0.3f, 0.9f, 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    change: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2B30)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = change,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

data class HistoryItem(
    val date: String,
    val status: String
)

@Composable
private fun HistoryCard(
    title: String,
    items: List<HistoryItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2B30)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (item.status) {
                            "Approved" -> Color(0xFF4CAF50)
                            "Denied" -> Color(0xFFE57373)
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}