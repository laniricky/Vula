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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vula.app.auth.ui.components.*

/**
 * RegisterScreen — Phase 3 OTP flow.
 *
 * Step 0: User enters username + phone, clicks "Send Code".
 * Step 1: User enters the 6-digit OTP code, clicks "Create Account".
 */
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(commonCountries.first { it.code == "US" }) }

    // Step 0: Phone entry, Step 1: OTP verification
    var currentStep by remember { mutableStateOf(0) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
        }
        if (authState is AuthState.CodeSent) {
            currentStep = 1
        }
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
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (currentStep == 0) "Join the Vula network" else "Enter the code we sent you",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
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
                }, label = "register_steps"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        // ── Step 0: Phone entry ───────────────────────────────────────────
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

                            Spacer(modifier = Modifier.height(16.dp))

                            val buttonState = when (authState) {
                                is AuthState.Loading -> ButtonState.Loading
                                is AuthState.Error   -> ButtonState.Error
                                else                 -> ButtonState.Idle
                            }

                            AnimatedSubmitButton(
                                text = "Send Code",
                                state = buttonState,
                                enabled = phoneNumber.length >= 5,
                                onClick = { viewModel.requestCode(selectedCountry.dialCode, phoneNumber) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // ── Step 1: OTP entry ─────────────────────────────────────────────
                        1 -> {
                            val fullPhone = "${selectedCountry.dialCode}${phoneNumber.trim()}"

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6) otpCode = it },
                                label = { Text("6-digit Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

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
                                is AuthState.Error   -> ButtonState.Error
                                else                 -> ButtonState.Idle
                            }

                            AnimatedSubmitButton(
                                text = "Create Account",
                                state = buttonState,
                                enabled = otpCode.length == 6,
                                onClick = { viewModel.verifyCode(fullPhone, otpCode) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = {
                                currentStep = 0
                                viewModel.clearError()
                            }) {
                                Text("Back to phone number", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            TextButton(onClick = { viewModel.requestCode(selectedCountry.dialCode, phoneNumber) }) {
                                Text("Resend code", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Login", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
