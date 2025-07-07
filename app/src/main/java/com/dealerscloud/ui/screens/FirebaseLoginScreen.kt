package com.dealerscloud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseLoginScreen(
    navController: NavController,
    viewModel: FirebaseLoginViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dealershipUrl by remember { mutableStateOf("https://yahauto.autodealerscloud.com") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (val currentState = authState) {
            is FirebaseAuthState.Authenticated -> {
                navController.navigate("chat") {
                    popUpTo("firebase_login") { inclusive = true }
                }
            }
            is FirebaseAuthState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = currentState.message,
                        duration = SnackbarDuration.Long,
                        actionLabel = "Dismiss"
                    )
                }
                viewModel.clearError()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "DealersCloud DCBEV",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "AI Assistant for DealersCloud",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Form title
                Text(
                    text = if (isCreatingAccount) "Create Account" else "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { 
                        Icon(Icons.Default.Email, contentDescription = "Email") 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { 
                        Icon(Icons.Default.Lock, contentDescription = "Password") 
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) 
                                    Icons.Default.VisibilityOff 
                                else 
                                    Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) 
                                    "Hide password" 
                                else 
                                    "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Dealership URL field
                OutlinedTextField(
                    value = dealershipUrl,
                    onValueChange = { dealershipUrl = it },
                    label = { Text("Dealership URL") },
                    supportingText = { 
                        Text("e.g., https://yourdealership.autodealerscloud.com") 
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit button
                Button(
                    onClick = {
                        if (isCreatingAccount) {
                            viewModel.createAccount(email, password, dealershipUrl)
                        } else {
                            viewModel.signInWithEmailPassword(email, password, dealershipUrl)
                        }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && 
                             dealershipUrl.isNotBlank() && authState !is FirebaseAuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (authState is FirebaseAuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (isCreatingAccount) "Create Account" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Toggle between sign in and create account
                TextButton(
                    onClick = { isCreatingAccount = !isCreatingAccount }
                ) {
                    Text(
                        text = if (isCreatingAccount) 
                            "Already have an account? Sign in" 
                        else 
                            "Need an account? Create one",
                        textAlign = TextAlign.Center
                    )
                }
                
                // Legacy login option
                TextButton(
                    onClick = { navController.navigate("dcbev_login") }
                ) {
                    Text(
                        text = "Use DCBEV legacy login",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}