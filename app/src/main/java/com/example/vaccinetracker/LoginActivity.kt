package com.example.vaccinetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.auth.FirebaseAuth

class LoginActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaccineTrackerTheme {
                LogInScreen(
                    onSignUpClick = {
                        val intent = Intent(this, SignUpActivity::class.java)
                        startActivity(intent)
                    },
                    onSignInSuccess = {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

private fun showErrorSnackBar(message: String, isError: Boolean) {
    // TODO: Show error snackbar
}

@Composable
fun LogInScreen(
    onSignUpClick: () -> Unit,
    onSignInSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var logInStatus by remember { mutableStateOf("") }  // Variable for displaying status message

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
             Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
             ) {
                Text(
                    text = "Welcome to Vaccine Tracker",
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontStyle = MaterialTheme.typography.headlineSmall.fontStyle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }


        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
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
                                // On successful sign in, set the login status
                                logInStatus = "Hello there"
                                onSignInSuccess()  // You can trigger additional actions here
                            } else {
                                errorMessage = task.exception!!.message.toString()
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
            Text("Sign In")
        }

        OutlinedButton(
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFE0B400)
            )

        ) {
            Text("Sign Up")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = it)
        }

        // Display the login status (Hello there) in a TextField (only while WIP)
        Text(
            text = logInStatus,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFF0000)
        )
    }
}

fun validate(email: String, password: String): Boolean {
    return email.isNotBlank() && password.isNotBlank()
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
fun LogInScreenPreview() {
    VaccineTrackerTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            LogInScreen(
                onSignUpClick = {},
                onSignInSuccess = {}
            )
        }
    }
}
