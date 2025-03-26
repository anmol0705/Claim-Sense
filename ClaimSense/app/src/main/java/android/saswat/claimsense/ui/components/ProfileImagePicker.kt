package android.saswat.claimsense.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ProfileImagePicker(
    currentImageUrl: String?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    showImageEditor: Boolean = false,
    onDismissImageEditor: () -> Unit = {},
    onSaveEditedImage: () -> Unit = {},
    isLoading: Boolean = false
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            onImageSelected(it)
        }
    }

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .border(2.dp, Color.Gray, CircleShape)
            .clickable(enabled = !isLoading) { imagePicker.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        } else if (selectedImageUri != null || !currentImageUrl.isNullOrEmpty()) {
            // Add loading state for AsyncImage
            var isImageLoading by remember { mutableStateOf(true) }
            
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedImageUri ?: currentImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false },
                    onError = { isImageLoading = false }
                )
                
                if (isImageLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Placeholder when no image is selected
            Text(
                text = "Add Photo",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showImageEditor && selectedImageUri != null) {
        AlertDialog(
            onDismissRequest = onDismissImageEditor,
            title = { Text("Edit Profile Picture") },
            text = {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Edit profile image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSaveEditedImage) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissImageEditor) {
                    Text("Cancel")
                }
            }
        )
    }
}