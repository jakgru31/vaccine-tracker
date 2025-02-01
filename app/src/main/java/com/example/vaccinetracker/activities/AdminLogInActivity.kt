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
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

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
    val successMessage by remember { mutableStateOf<String?>(null) }

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
                if (validate(email, password))
                {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    {
                        errorMessage = "Please enter a valid email address"
                        return@Button // Stop here if email is invalid
                    }

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
                                                    errorMessage =
                                                        "You are not authorized as an admin."
                                                    FirebaseAuth.getInstance().signOut()
                                                }
                                            } else {
                                                errorMessage = "User document not found."
                                                FirebaseAuth.getInstance()
                                                    .signOut() // Handle as needed
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage =
                                                "Error checking admin status: ${exception.message}"
                                            Log.e(
                                                "FirestoreError",
                                                "Error checking admin status: ${exception.message}",
                                                exception
                                            )
                                        }
                                } else {
                                    errorMessage =
                                        "An unexpected error occurred. Please try again." // User should not be null after auth
                                }
                            }
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

        Spacer(modifier = Modifier.padding(8.dp))

        errorMessage?.let {
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = it, color = Color.Red)
        }

        successMessage?.let {
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = it, color = Color.Green)
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
