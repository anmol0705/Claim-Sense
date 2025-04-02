package android.saswat.claimsense.ui.Main

import android.net.Uri
import android.saswat.claimsense.ui.components.ProfileImagePicker
import android.saswat.claimsense.viewmodel.AuthViewModel
import android.saswat.claimsense.viewmodel.UpdateState
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileContent(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val userData by authViewModel.userData.collectAsState()
    val updateState by authViewModel.updateState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(userData?.username ?: "") }
    var editedDriverLicense by remember { mutableStateOf(userData?.driverLicense ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageEditor by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(userData) {
        editedUsername = userData?.username ?: ""
        editedDriverLicense = userData?.driverLicense ?: ""
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.Success -> {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                isEditing = false
                authViewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                Toast.makeText(context, (updateState as UpdateState.Error).message, Toast.LENGTH_SHORT).show()
                authViewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                // Profile Image section at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileImagePicker(
                        currentImageUrl = userData?.profileImageUrl,
                        onImageSelected = { uri ->
                            selectedImageUri = uri
                            showImageEditor = true
                        },
                        modifier = Modifier
                            .size(140.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF00B3CC)
                                    )
                                ),
                                shape = RoundedCornerShape(100)
                            ),
                        showImageEditor = showImageEditor,
                        onDismissImageEditor = { showImageEditor = false },
                        onSaveEditedImage = {
                            selectedImageUri?.let { uri ->
                                authViewModel.updateProfileImage(uri) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Profile image updated!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showImageEditor = false
                        }
                    )
                }

                // Info Card with enhanced styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        // Add subtle animation when appearing
                        .animateContentSize()
                        // Add elevation effect
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E1E1E),
                                        Color(0xFF252525)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 1000f)
                                )
                            )
                            // Add subtle border glow
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF303030),
                                        Color(0xFF00E5FF).copy(alpha = 0.3f),
                                        Color(0xFF303030)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Header with accent line
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Your Information",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(2.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF),
                                                        Color(0xFF00E5FF).copy(alpha = 0.3f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(1.dp)
                                            )
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        if (isEditing) {
                                            if (editedUsername.isNotBlank()) {
                                                authViewModel.updateUserData(
                                                    editedUsername,
                                                    editedDriverLicense
                                                )
                                            } else {
                                                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            isEditing = true
                                        }
                                    },
                                    enabled = updateState !is UpdateState.Loading,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF00E5FF)
                                    ),
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF2A2A2A),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp)
                                ) {
                                    if (updateState is UpdateState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF00E5FF),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        when {
                                            updateState is UpdateState.Loading -> "Saving..."
                                            isEditing -> "Save"
                                            else -> "Edit"
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Edit mode or display mode content
                            if (isEditing) {
                                // Edit Fields with enhanced styling
                                EditField(
                                    label = "Username",
                                    value = editedUsername,
                                    onValueChange = { editedUsername = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                EditField(
                                    label = "Driver's License",
                                    value = editedDriverLicense,
                                    onValueChange = { editedDriverLicense = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        isEditing = false
                                        authViewModel.resetUpdateState()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2A2A2A),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel")
                                }
                            } else {
                                // Enhanced info display
                                EnhancedInfoRow(label = "Username", value = userData?.username ?: "Not set")
                                EnhancedInfoRow(label = "Email", value = userData?.email ?: "Not set")
                                EnhancedInfoRow(label = "Driver's License", value = userData?.driverLicense ?: "Not set")
                                EnhancedInfoRow(label = "User ID", value = userData?.userId ?: "Not set", isLast = true)
                            }
                        }
                    }
                }

                // Enhanced Sign Out Button
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(56.dp)
                        .shadow(
                            elevation = 4.dp,
                            spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF00B3CC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color.Black
                            )
                            Text(
                                "Sign Out",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.7f),
                            Color(0xFF00E5FF).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF2A2A2A),
                focusedContainerColor = Color(0xFF2A2A2A),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF00E5FF),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun EnhancedInfoRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF252525),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}