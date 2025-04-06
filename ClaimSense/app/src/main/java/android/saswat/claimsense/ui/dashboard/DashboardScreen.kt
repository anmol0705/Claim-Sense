package android.saswat.claimsense.ui.dashboard

import android.saswat.claimsense.ui.components.AnimatedBarChart
import android.saswat.claimsense.viewmodel.AuthViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.saswat.claimsense.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    authViewModel: AuthViewModel
) {
    val userData by authViewModel.userData.collectAsState()
    val backgroundColor = Color(0xFF1B1B1F)
    val cardColor = Color(0xFF252836)
    val accentColor = Color(0xFF00E5FF)

    val montserratFontFamily = FontFamily(
        Font(R.font.montserratregular, FontWeight.Normal),
        Font(R.font.montserratsemibold, FontWeight.SemiBold),
        Font(R.font.montserratextrabold, FontWeight.ExtraBold),
        Font(R.font.montserratblackitalic, FontWeight.Bold),
        Font(R.font.montserratbolditalic, FontWeight.Bold),
        Font(R.font.montserratitalic, FontWeight.Medium),
        Font(R.font.montserratlightitalic, FontWeight.Light),
        Font(R.font.montserratmediumitalic, FontWeight.Medium)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // User Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = userData?.username?.uppercase() ?: "ANIMES",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = montserratFontFamily
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "USER ID : ${userData?.userId?.take(8) ?: "XV8WFDJT"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                fontFamily = montserratFontFamily
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "LICENSE NO : wfvsuzu2ruww88su",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                fontFamily = montserratFontFamily
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "INSURANCE NO : OR02-0120",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                fontFamily = montserratFontFamily
                            )
                        }

                        // Profile Image
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2530))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userData?.profileImageUrl ?: "")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Combined Premium and Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Insurance Premium Section
                        Text(
                            text = "INSURANCE PREMIUM",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.LightGray,
                            fontFamily = montserratFontFamily
                        )

                        Text(
                            text = "$415.24",
                            fontSize = 36.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = montserratFontFamily,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = Color(0xFF353845),
                            thickness = 1.dp
                        )

                        // Chart Section
                        Text(
                            text = "USAGE DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF8A8D93),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = montserratFontFamily,
                            modifier = Modifier.padding(bottom = 12.dp),
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            AnimatedBarChart(
                                data = listOf(0.25f, 0.55f, 0.3f, 0.85f, 0.45f, 0.2f, 0.7f, 0.45f),
                                modifier = Modifier.fillMaxSize(),
                                barColor = accentColor,
                                labels = listOf("M", "T", "W", "T", "F", "S", "S", "st")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { /* More Details Action */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E2530),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Text(
                                    text = "More Details",
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = montserratFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}