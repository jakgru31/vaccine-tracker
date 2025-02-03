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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.vaccinetracker.collections.User
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * SignUpActivity handles the user registration process for the Vaccine Tracker app.
 * It sets up the UI and manages user input, validation, and Firebase authentication.
 */
class SignUpActivity : ComponentActivity() {
    /**
     * Initializes the activity and sets the content to the registration screen.
     */
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

/**
 * Displays the user registration screen with input fields, validation, and submission functionality.
 *
 * @param onSignUpSuccess Callback invoked upon successful registration.
 */
@Composable
fun RegistrationScreen(onSignUpSuccess: () -> Unit = {}) {
    // Firebase Authentication and Firestore instances
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // User input states
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    /**
     * Validates user input and registers a new user with Firebase Authentication.
     */
    @SuppressLint("SuspiciousIndentation")
    fun registerUser() {
        if (email.isBlank() || password != confirmPassword || name.isBlank() || dob.isBlank() || selectedGender.isBlank()) {
            errorMessage = "Please fill out all fields correctly."
            return
        }

        if (!emailValidator(email)) {
            errorMessage = "Invalid email format."
            return
        }
        if (!passwordValidator(password)) {
            errorMessage = "Invalid password format. Password must have at least 8 characters, a mix of upper and lowercase, a number and a special character."
            return
        }
        if (!dobValidator(dob)) {
            errorMessage = "Invalid date of birth."
            return
        }
        if (!nameSurnameValidator(name, surname)) {
            errorMessage = "Invalid name or surname."
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = User(
                        id = userId,
                        email = email.trim(),
                        name = name,
                        surname = surname,
                        dateOfBirth = dob,
                        gender = selectedGender,
                        vaccinationRecords = mutableListOf(),
                        appointments = mutableListOf(),
                        isAdmin = false
                    )
                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            successMessage = "Registration successful!"
                            onSignUpSuccess()
                        }
                        .addOnFailureListener { exception ->
                            errorMessage = "Error registering user: ${exception.message}"
                        }
                } else {
                    errorMessage = task.exception?.message ?: "Registration failed."
                }
            }
    }

    // UI components
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign Up for Vaccine Tracker",
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input fields for user details
        TextField(value = name, onValueChange = { name = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = surname, onValueChange = { surname = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        // Date of Birth Picker
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth") }, readOnly = true, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showDatePicker = true }) { Text("Select Date") }
        }

        if (showDatePicker) {
            DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
                dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                showDatePicker = false
            }, year, month, day).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Password fields
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(modifier = Modifier.height(8.dp))

        // Gender selection
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gender:")
            Spacer(modifier = Modifier.width(8.dp))
            GenderRadioButton(selectedGender, "Male") { selectedGender = "Male" }
            GenderRadioButton(selectedGender, "Female") { selectedGender = "Female" }
            GenderRadioButton(selectedGender, "Other") { selectedGender = "Other" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Register button
        Button(onClick = { registerUser() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error and success messages
        errorMessage?.let { Text(text = it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
        successMessage?.let { Text(text = it, color = Color.Green, modifier = Modifier.padding(top = 8.dp)) }
    }
}

/**
 * Displays a gender selection radio button.
 *
 * @param selectedGender The currently selected gender.
 * @param label The label for the radio button.
 * @param onSelect Callback invoked when the radio button is selected.
 */
@Composable
fun GenderRadioButton(selectedGender: String, label: String, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedGender == label, onClick = onSelect)
        Text(text = label, modifier = Modifier.padding(end = 8.dp))
    }
}

/**
 * Validates the name and surname.
 *
 * @return true if both name and surname contain only letters.
 */
private fun nameSurnameValidator(name: String, surname: String): Boolean {
    val namePattern = Regex("^[a-zA-Z]+")
    return namePattern.matches(name) && namePattern.matches(surname)
}

/**
 * Validates the email format.
 *
 * @return true if the email format is valid.
 */
private fun emailValidator(email: String): Boolean {
    return email.contains("@") && email.contains(".com") && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Validates the password format.
 *
 * @return true if the password meets the complexity requirements.
 */
private fun passwordValidator(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}")
    return passwordPattern.matches(password)
}

/**
 * Validates the date of birth to ensure the user is 18 years or older.
 *
 * @return true if the user is 18 years or older.
 */
private fun dobValidator(dob: String): Boolean {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dobDate = sdf.parse(dob)
    val calendar = Calendar.getInstance()
    calendar.time = dobDate
    val yearOfBirth = calendar.get(Calendar.YEAR)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return (currentYear - yearOfBirth > 18) || (currentYear - yearOfBirth == 18 && calendar.get(Calendar.DAY_OF_YEAR) <= Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
}

/**
 * A placeholder coroutine function.
 */
suspend fun doS() {}

/**
 * Preview for the RegistrationScreen composable.
 */
@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    VaccineTrackerTheme {
        Scaffold {
            RegistrationScreen(onSignUpSuccess = {})
        }
    }
}
