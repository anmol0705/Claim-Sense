package android.saswat.claimsense.ui.signInSignUp

import android.content.Context
import android.net.Uri
import android.saswat.claimsense.R
import android.saswat.claimsense.ui.components.ProfileImagePicker
import android.saswat.claimsense.viewmodel.AuthViewModel
import android.saswat.claimsense.viewmodel.ImageLoadState
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onSignInClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var driverLicense by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageEditor by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val imageLoadState by authViewModel.imageLoadState.collectAsState()
    val isLoading = authState is AuthViewModel.AuthState.Loading || 
                  imageLoadState is ImageLoadState.Loading

    LaunchedEffect(authViewModel.authState) {
        authViewModel.authState.collect { state ->
            when (state) {
                is AuthViewModel.AuthState.Success -> {
                    showToast(context, "Successfully signed up!")
                    onSignUpSuccess()
                }
                is AuthViewModel.AuthState.Error -> {
                    errorMessage = state.message
                    showToast(context, state.message)
                    showError = true
                }
                is AuthViewModel.AuthState.Loading -> {
                    showToast(context, "Creating account...")
                }
                else -> {}
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    "Terms and Conditions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    """
                    Welcome to ClaimSense!
                    
                    1. By using our app, you agree to:
                       - Provide accurate information
                       - Maintain account security
                       - Use the service legally
                    
                    2. Your privacy matters:
                       - We protect your data
                       - Only collect necessary information
                       - Never share without permission
                    
                    3. Account Guidelines:
                       - One account per user
                       - Keep credentials confidential
                       - Report unauthorized access
                    
                    4. Driver's License:
                       - Must be valid and current
                       - Used for verification only
                       - Stored securely
                    
                    Please read the full terms on our website.
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        termsAccepted = true
                        showTermsDialog = false
                    }
                ) {
                    Text(
                        "Accept",
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTermsDialog = false }
                ) {
                    Text(
                        "Decline",
                        color = Color.Gray
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF5F5F5)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top icon with animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "iconTransition")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconScale"
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = "Star",
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale)
                        .align(Alignment.TopEnd)
                )
            }
            // Header section
            Text(
                text = "Create account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Row(
               horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ){
                ProfileImagePicker(
                    currentImageUrl = null,
                    onImageSelected = { uri ->
                        selectedImageUri = uri
                        showImageEditor = true
                    },
                    showImageEditor = showImageEditor,
                    onDismissImageEditor = { showImageEditor = false },
                    onSaveEditedImage = { showImageEditor = false },
                    isLoading = imageLoadState is ImageLoadState.Loading
                )
            }


            // Username field with dark labels
            Text(
                text = "Username",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
            )

            // Email field
            Text(
                text = "Email",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            // Password field
            Text(
                text = "Password",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                            ),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Driver's License field
            Text(
                text = "Driver's License Number",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = driverLicense,
                onValueChange = { driverLicense = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Terms and conditions checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.Gray
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showTermsDialog = true }
                ) {
                    Text(
                        text = "I accept the ",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "terms and privacy policy",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Sign Up button
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty() && driverLicense.isNotEmpty()) {
                        if (termsAccepted) {
                            authViewModel.signUpWithEmailPassword(
                                email = email,
                                password = password,
                                username = username,
                                driverLicense = driverLicense,
                                profileImageUri = selectedImageUri,
                                onComplete = { success ->
                                    if (!success) {
                                        showError = true
                                        errorMessage = "Failed to create account"
                                    } else {
                                        onSignUpSuccess()
                                        showToast(context = context, "Account created successfully")
                                    }
                                }
                            )
                        } else {
                            showError = true
                            errorMessage = "Please accept the terms and privacy policy"
                            showToast(context, errorMessage)
                        }
                    } else {
                        showError = true
                        errorMessage = "Please fill in all fields"
                        showToast(context, errorMessage)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Create account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Sign in link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onSignInClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Log in",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Preview
@Composable
fun PreviewSignUp() {
    SignUpScreen(
        onSignUpSuccess = { },
        onSignInClick = { }
    )
}