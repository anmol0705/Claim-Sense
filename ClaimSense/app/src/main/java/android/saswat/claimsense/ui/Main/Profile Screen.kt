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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Toast.makeText(
                    context,
                    (updateState as UpdateState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Profile header


            // Single card with two sections
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF212C3B).copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Hello, Username text
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HELLO, ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.LightGray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = userData?.username?.uppercase() ?: "USERNAME",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Profile Image
                        Box(
                            modifier = Modifier.fillMaxWidth(),
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
                                        width = 3.dp,
                                        color = Color(0xFF0E0D0C),
                                        shape = CircleShape
                                    ),
                                showImageEditor = showImageEditor,
                                onDismissImageEditor = { showImageEditor = false },
                                onSaveEditedImage = {
                                    selectedImageUri?.let { uri ->
                                        authViewModel.updateProfileImage(uri) { success ->
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Profile image updated!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    showImageEditor = false
                                }
                            )
                        }
                    }

                    // User Information section with blur effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    clip = true
                                )
                                .background(
                                    Color(0xFF2E4053),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(24.dp)
                        ) {
                            if (isEditing) {
                                Column {
                                    TextField(
                                        value = editedUsername,
                                        onValueChange = { editedUsername = it },
                                        label = { Text("Username") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = Color(0xFF2A3440),
                                            focusedContainerColor = Color(0xFF2A3440),
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            cursorColor = Color(0xFF00E5FF),
                                            unfocusedTextColor = Color.White,
                                            focusedTextColor = Color.White,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedLabelColor = Color(0xFF00E5FF)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Driver's License field
                                    TextField(
                                        value = editedDriverLicense,
                                        onValueChange = { editedDriverLicense = it },
                                        label = { Text("Driver's License") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 24.dp)
                                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = Color(0xFF2A3440),
                                            focusedContainerColor = Color(0xFF2A3440),
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            cursorColor = Color(0xFF00E5FF),
                                            unfocusedTextColor = Color.White,
                                            focusedTextColor = Color.White,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedLabelColor = Color(0xFF00E5FF)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Save and Cancel buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            onClick = {
                                                isEditing = false
                                                authViewModel.resetUpdateState()
                                            }
                                        ) {
                                            Text("CANCEL", color = Color.LightGray)
                                        }

                                        Button(
                                            onClick = {
                                                if (editedUsername.isNotBlank()) {
                                                    authViewModel.updateUserData(
                                                        editedUsername,
                                                        editedDriverLicense
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Username cannot be empty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            enabled = updateState !is UpdateState.Loading,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF00E5FF),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (updateState is UpdateState.Loading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.Black,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text("SAVE")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "USER ID :",
                                                fontSize = 16.sp,
                                                color = Color.LightGray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color(0xFF273548),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = userData?.userId?.takeLast(6)
                                                        ?: "121121",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "EMAIL :",
                                                fontSize = 16.sp,
                                                color = Color.LightGray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color(0xFF273548),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = userData?.email ?: "email@example.com",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "DRIVER'S LICENSE",
                                                fontSize = 16.sp,
                                                color = Color.LightGray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color(0xFF273548),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                                            ) {
                                                Text(
                                                    text = userData?.driverLicense ?: "OR-02-68792",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Edit button with elevation
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Button(
                                            onClick = { isEditing = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4B5563),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(40.dp)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Text("EDIT")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
