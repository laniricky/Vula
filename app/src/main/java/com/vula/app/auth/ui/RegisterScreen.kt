package com.vula.app.auth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import com.vula.app.auth.ui.components.*

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var selectedCountry by remember { mutableStateOf(commonCountries.first { it.code == "US" }) }
    
    // Step 0: User Info, Step 1: Password
    var currentStep by remember { mutableStateOf(0) }
    
    val authState by viewModel.authState.collectAsState()

    // Navigate away on success
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
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
                text = "Join the Vula network",
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
                }, label = "register_steps"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        0 -> {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
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
                                enabled = username.isNotBlank() && phoneNumber.length >= 5,
                                onClick = { currentStep = 1 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { 
                                    password = it 
                                    passwordError = if (password != confirmPassword && confirmPassword.isNotEmpty()) "Passwords do not match" else null
                                },
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
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it 
                                    passwordError = if (password != confirmPassword) "Passwords do not match" else null
                                },
                                label = { Text("Confirm Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = passwordError != null,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            
                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            if (authState is AuthState.Error) {
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val buttonState = when {
                                passwordError != null -> ButtonState.Error
                                authState is AuthState.Loading -> ButtonState.Loading
                                authState is AuthState.Success -> ButtonState.Success
                                authState is AuthState.Error -> ButtonState.Error
                                else -> ButtonState.Idle
                            }

                            AnimatedSubmitButton(
                                text = "Register",
                                state = buttonState,
                                enabled = password.length >= 6 && confirmPassword.length >= 6 && passwordError == null,
                                onClick = { 
                                    if (password == confirmPassword) {
                                        viewModel.register(selectedCountry.dialCode, phoneNumber, username, password) 
                                    } else {
                                        passwordError = "Passwords do not match"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(onClick = { 
                                currentStep = 0
                            }) {
                                Text("Back to info", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
