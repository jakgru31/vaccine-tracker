package com.example.vaccinetracker.activities
import addNewUserToDatabase
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.material3.RadioButton
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.data.Vaccine
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

     fun registerUser() {
        if (email.isBlank() || password != confirmPassword || name.isBlank() || dob.isBlank() || selectedGender.isBlank()) {
            errorMessage = "Please fill out all fields correctly."
            return
        }

        // Add validation
        if (!emailValidator(email)) {
            errorMessage = "Invalid email format."
            return
        }
        if (!passwordValidator(password)) {
            errorMessage = "Invalid password format. Password must have at least 8 characters, a mix of upper and lowercase, a number and a special character."
            return
        }
        // Add validation check
        if (!dobValidator(dob)) {
            errorMessage = "Invalid date of birth."
            return
        }


         val newUser = User(
             id = "",
             email = email.trim(),
             password = password.trim(),
             name = name,
             surname = surname,
             gender = selectedGender,
             dateOfBirth = dob,
             vaccinationHistories = mutableListOf()
         )

         CoroutineScope(Dispatchers.IO).launch {
             val registrationSuccessful = addNewUserToDatabase(newUser) // Use the return value!
             withContext(Dispatchers.Main) {
                 if (registrationSuccessful) {
                     successMessage = "Registration successful!"
                     onSignUpSuccess() // Only navigate on success
                 }
             }
         }




/*auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val newUser = User("", email, password, name, surname, selectedGender,dob, mutableListOf())
                    addNewUserToDatabase(newUser)
                    successMessage = "Registration successful!"
                    onSignUpSuccess()
                } else {
                    errorMessage = task.exception?.message ?: "Registration failed."
                }
            }.addOnFailureListener { exception ->
                errorMessage = "Error registering user: ${exception.message}"
            }*/
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showDatePicker = true }) {
                Text("Select Date")
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    showDatePicker = false
                },
                year,
                month,
                day
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gender:")
            Spacer(modifier = Modifier.width(8.dp))
            GenderRadioButton(selectedGender, "Male") { selectedGender = "Male" }
            GenderRadioButton(selectedGender, "Female") { selectedGender = "Female" }
            GenderRadioButton(selectedGender, "Other") { selectedGender = "Other" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { registerUser() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color.Black
            )
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
        successMessage?.let {
            Text(text = it, color = Color.Green, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun GenderRadioButton(selectedGender: String, label: String, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selectedGender == label,
            onClick = onSelect
        )
        Text(text = label, modifier = Modifier.padding(end = 8.dp))
    }
}
private fun nameSurnameValidator(name: String, surname: String): Boolean {
    val namePattern = Regex("^[a-zA-Z]+$") // Ensure only letters
    return namePattern.matches(name) && namePattern.matches(surname)
}

private fun emailValidator(email: String): Boolean {
    // Ensure the email contains "@" and ".com"
    return email.contains("@") && email.contains(".com") &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun passwordValidator(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}$")
    return passwordPattern.matches(password)
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

suspend fun doS()
{

}