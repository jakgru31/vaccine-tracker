/**
 * Admin Login Activity for Vaccine Tracker App
 *
 * This file defines the `AdminLogInActivity` and its associated UI components.
 * It handles the authentication of admin users using Firebase Authentication and
 * verifies their admin status through Firestore before granting access to the admin dashboard.
 *
 * Key Components:
 * - `AdminLogInActivity`: The entry point for the admin login screen.
 * - `AdminLogInScreen`: A composable function providing the UI for admin authentication.
 * - Firebase Authentication: Handles email-password login.
 * - Firestore Database: Verifies if the logged-in user has admin privileges.
 *
 * Features:
 * - Email and password input fields with validation.
 * - Firebase authentication and admin status verification.
 * - Animated error and success messages.
 * - Smooth UI with Material 3 components.
 * - Navigation to the admin dashboard (`AdminActivity`) upon successful login.
 * - A button to switch to the user login screen (`LoginActivity`).
 *
 * Dependencies:
 * - Firebase Authentication (`FirebaseAuth`)
 * - Firestore Database (`FirebaseFirestore`)
 * - Jetpack Compose UI components
 * - Material 3 design system
 *
 * Error Handling:
 * - Displays appropriate error messages for invalid input or authentication failures.
 * - Ensures only admin users can proceed beyond the login screen.
 *
 * Author: [Your Name]
 * Date: [Date]
 */

package com.example.vaccinetracker.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class AdminLogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaccineTrackerTheme {
                AdminLogInScreen(
                    onSignInSuccess = {
                        val intent = Intent(this, AdminActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AdminLogInScreen(
    onSignInSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Smooth animation logic for error and success message disappear
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            delay(7000) // Keep the message visible for 7 seconds
            errorMessage = null
            successMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Log in as Admin to Vaccine Tracker",
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp)
        )

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = {
                // Validate fields before Firebase authentication
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please fill in both email and password."
                    return@Button
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Please enter a valid email address."
                    return@Button
                }

                // Firebase sign-in
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(user.uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val isAdmin = document.getBoolean("admin") ?: false
                                            if (isAdmin) {
                                                onSignInSuccess()
                                            } else {
                                                errorMessage = "You are not authorized as an admin."
                                                FirebaseAuth.getInstance().signOut()
                                            }
                                        } else {
                                            errorMessage = "User document not found."
                                            FirebaseAuth.getInstance().signOut()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        errorMessage = "Error checking admin status: ${exception.message}"
                                    }
                            } else {
                                errorMessage = "An unexpected error occurred. Please try again."
                            }
                        } else {
                            errorMessage = "Incorrect password or email"
                        }
                    }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007BFF),
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log In")
        }

        Spacer(modifier = Modifier.padding(200.dp))

        val context = LocalContext.current

        Button(
            onClick = {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007BFF),
                contentColor = Color.White
            ),
            modifier = Modifier.focusGroup()
        ) {
            Text("I am user")
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
fun AdminLogInScreenPreview() {
    VaccineTrackerTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            AdminLogInScreen(
                onSignInSuccess = {}
            )
        }
    }
}
