package com.example.vaccinetracker
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.tooling.preview.Preview
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import java.util.Calendar

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            RegistrationScreen(
            )
        }
    }
}

@Composable
fun RegistrationScreen() {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // var dob by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Emailll") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isNotBlank() && emailValidator(email)
                    && password.isNotBlank() && password == confirmPassword
                    && passwordValidator(password)) {
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                successMessage = "Registration successful!"
                                errorMessage = null
                                //TODO: Navigate to Login screen

                            } else {
                                errorMessage = task.exception?.message
                                successMessage = null
                            }
                        }
                } else {
                    errorMessage = "Please check your inputs."
                    successMessage = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        // TODO: Snackbar for error/success message
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it)
        }
        successMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it)
        }
    }
}
private fun passwordValidator(password: String): Boolean {
    return (password.length >= 8) && (password.contains(Regex("[0-9]"))) && (password.contains("[A-Z]"))
}
private fun emailValidator(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    VaccineTrackerTheme {
        Scaffold { innerPadding ->
            RegistrationScreen()
        }
    }
}