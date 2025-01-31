package com.example.vaccinetracker.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
                if (validate(email, password)) {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Proceed
                                print("hello there ")
                                //checkIfUserIsAdmin(email, onSignInSuccess)
                            } else {
                                // Show specific error message
                                errorMessage = task.exception?.localizedMessage ?: "An error occurred"
                            }
                        }
                } else {
                    errorMessage = "Please fill out all the fields"
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log In")
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

/*
fun checkIfUserIsAdmin(email: String, onSignInSuccess: () -> Unit) {
    // Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    // Query Firestore to check if this email is in the "admins" collection
    db.collection("admins")
        .whereEqualTo("email", email)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) {
                // User is an admin, proceed to the Admin Activity
                onSignInSuccess()
            } else {
                // User is not an admin
                // You can show an error or navigate to a different activity
                println("User is not an admin")
            }
        }
        .addOnFailureListener { exception ->
            println("Error checking admin status: $exception")
        }
}
*/


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
