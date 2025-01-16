package com.example.vaccinetracker.activities
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.material3.RadioButton
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            VaccineTrackerTheme {
                RegistrationScreen(
                    onSignUpSuccess = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    onSignUpSuccess: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val db = FirebaseFirestore.getInstance()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
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
                    text = "Sign Up for Vaccine Tracker",
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontStyle = MaterialTheme.typography.headlineSmall.fontStyle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("First name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Last name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth") },
                readOnly = true,
                modifier = Modifier
                    .weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Select Date")
            }
        }
        if (showDatePicker) {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    showDatePicker = false
                },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.setOnDismissListener {
                showDatePicker = false
            }
            datePickerDialog.show()
        }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gender: ", modifier = Modifier.padding(end = 8.dp))
            RadioButton(
                selected = selectedGender == "Male",
                onClick = { selectedGender = "Male" }
            )
            Text("Male", modifier = Modifier.padding(end = 16.dp))
            RadioButton(
                selected = selectedGender == "Female",
                onClick = { selectedGender = "Female" }
            )
            Text("Female", modifier = Modifier.padding(end = 16.dp))
            RadioButton(
                selected = selectedGender == "Other",
                onClick = { selectedGender = "Other" }
            )
            Text("Other")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val k = passwordValidator(password)
                if (email.isNotBlank() && emailValidator(email)
                    && password.isNotBlank() && password == confirmPassword
                    && passwordValidator(password) && nameSurnameValidator(name, surname) && name.isNotBlank()
                    && surname.isNotBlank() && dob.isNotBlank() && dobValidator(dob) && selectedGender.isNotBlank()
                    ) {
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = User(
                                    id = auth.currentUser?.uid ?: "",
                                    email = email,
                                    name = name,
                                    surname = surname,
                                    gender = selectedGender,
                                    dateOfBirth = dob,
                                    certificates = mutableListOf()
                                )
                                db.collection("users")
                                    .document(user.id).set(user)
                                    .addOnSuccessListener {
                                        successMessage = "User registered successfully"
                                    }
                                onSignUpSuccess()

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
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color.Black
            ),
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
    return (password.length >= 8) &&
            (password.contains(Regex("[0-9]"))) &&
            (password.contains(Regex("[A-Z]"))) &&
            (password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]")))

}
private fun emailValidator(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun nameSurnameValidator(name: String, surname: String): Boolean {
    val namePattern = Regex("^[a-zA-Z]+\$")
    return namePattern.matches(name) && namePattern.matches(surname)
}


private fun dobValidator(dob: String): Boolean {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dobDate = sdf.parse(dob)
    val calendar = Calendar.getInstance()
    calendar.time = dobDate
    val yearOfBirth = calendar.get(Calendar.YEAR)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return (currentYear - yearOfBirth > 18) || (currentYear - yearOfBirth == 18 && calendar.get(Calendar.DAY_OF_YEAR) <= Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    VaccineTrackerTheme {
        Scaffold { innerPadding ->
            RegistrationScreen(
                onSignUpSuccess = {}
            )
        }
    }
}
