package com.vula.app.auth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.auth.ui.components.*

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(commonCountries.first { it.code == "US" }) }
    
    // Step 0: Phone, Step 1: Password
    var currentStep by remember { mutableStateOf(0) }

    val authState by viewModel.authState.collectAsState()
    val resetState by viewModel.resetState.collectAsState()

    // Navigate away on success
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.clearError()
            onLoginSuccess()
        }
    }

    // Forgot password dialog
    if (showResetDialog) {
        ForgotPasswordDialog(
            initialPhoneNumber = phoneNumber,
            initialCountryCode = selectedCountry.dialCode,
            resetState = resetState,
            onDismiss = {
                showResetDialog = false
                viewModel.clearResetState()
            },
            onSubmit = { code, num -> viewModel.resetPassword(code, num) }
        )
    }

    DynamicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in to continue to Vula",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(500), initialOffsetX = { it }) + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(500), targetOffsetX = { -it }) + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(500), initialOffsetX = { -it }) + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(500), targetOffsetX = { it }) + fadeOut()
                    }
                }, label = "login_steps"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        0 -> {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    CountryCodeSelector(
                                        selectedCountry = selectedCountry,
                                        onCountrySelected = { selectedCountry = it }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            AnimatedSubmitButton(
                                text = "Next",
                                state = ButtonState.Idle,
                                enabled = phoneNumber.length >= 5, // Simple check
                                onClick = { currentStep = 1 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            // Forgot password link
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(onClick = { showResetDialog = true }) {
                                    Text("Forgot password?", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AnimatedVisibility(visible = authState is AuthState.Error) {
                                Text(
                                    text = (authState as? AuthState.Error)?.message ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            val buttonState = when (authState) {
                                is AuthState.Loading -> ButtonState.Loading
                                is AuthState.Success -> ButtonState.Success
                                is AuthState.Error -> ButtonState.Error
                                else -> ButtonState.Idle
                            }

                            AnimatedSubmitButton(
                                text = "Login",
                                state = buttonState,
                                enabled = password.length >= 6,
                                onClick = { viewModel.login(selectedCountry.dialCode, phoneNumber, password) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(onClick = { 
                                currentStep = 0
                                viewModel.clearError()
                            }) {
                                Text("Back to phone number", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Don't have an account? Register",
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialPhoneNumber: String,
    initialCountryCode: String,
    resetState: ResetState,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf(initialPhoneNumber) }
    var selectedCountry by remember { mutableStateOf(commonCountries.firstOrNull { it.dialCode == initialCountryCode } ?: commonCountries.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                when (resetState) {
                    is ResetState.Sent -> {
                        Text(
                            "✅ If that username is registered, a reset email has been sent. Check the email linked to your account.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            "Enter your phone number and we'll send a password reset link to the associated account.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                CountryCodeSelector(
                                    selectedCountry = selectedCountry,
                                    onCountrySelected = { selectedCountry = it }
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        if (resetState is ResetState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = resetState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (resetState is ResetState.Sent) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                TextButton(
                    onClick = { onSubmit(selectedCountry.dialCode, phoneNumber) },
                    enabled = resetState !is ResetState.Loading && phoneNumber.isNotBlank()
                ) {
                    if (resetState is ResetState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Link")
                    }
                }
            }
        },
        dismissButton = {
            if (resetState !is ResetState.Sent) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
