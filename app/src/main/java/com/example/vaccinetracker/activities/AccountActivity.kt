package com.example.vaccinetracker.activities

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vaccinetracker.collections.User
import com.example.vaccinetracker.collections.Vaccine
import com.example.vaccinetracker.data.UserRepository
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import userMakesAppointment
import userMakesVaccination
import java.time.LocalDateTime
import java.util.Calendar

//import userMakesVaccination

class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaccineTrackerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Vaccine Tracker (Version 0.1)") })
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier) {
    val coroutineScope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }
        composable("vaccines") { VaccinesScreen(coroutineScope = coroutineScope)}
        composable("certificates") { CertificatesScreen() }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Vaccines,
        BottomNavItem.Certificates
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    when (item) {
                        BottomNavItem.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                        BottomNavItem.Vaccines -> Icon(Icons.Default.Check, contentDescription = "Vaccines")
                        BottomNavItem.Certificates -> Icon(Icons.Default.Check, contentDescription = "Certificates")
                    }
                },
                label = { Text(text = item.title) }
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String) {
    object Home : BottomNavItem("home", "Home")
    object Vaccines : BottomNavItem("vaccines", "Vaccines")
    object Certificates : BottomNavItem("certificates", "Certificates")
}

@Composable
fun HomeScreen() {
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            isLoading = true
            errorMessage = null
            userRepository.fetchUserData(userId) { fetchedUser ->
                if (fetchedUser != null) {
                    userData = fetchedUser
                } else {
                    errorMessage = "Failed to fetch user data."
                }
                isLoading = false
            }
        } ?: run {
            errorMessage = "No user logged in."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Home", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Text(text = "Loading...")
            errorMessage != null -> Text(text = errorMessage ?: "An unknown error occurred.")
            userData != null -> {
                print(userData?.id)
                Text(text = "Welcome, ${userData?.name ?: "User"}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Email: ${userData?.email ?: "Not available"}")
                Text(text = "Surname: ${userData?.surname ?: "Not available"}")
                Text(text = "Gender: ${userData?.gender ?: "Not available"}")
                Text(text = "Date of Birth: ${userData?.dateOfBirth ?: "Not available"}")

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Certificates:")
                if (userData?.vaccinationRecords.isNullOrEmpty()) {
                    Text(text = "No certificates available.")
                } else {
                    userData?.vaccinationRecords?.forEach { certificate ->
                        Text(text = "- ${certificate}")
                    }
                }
            }
            else -> Text(text = "No data available.")
        }
    }
}

@Composable
fun VaccinesScreen(
    coroutineScope: CoroutineScope
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedVaccine by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<Timestamp?>(null) }
    val vaccines = remember { mutableStateListOf<Vaccine>() }

    val context = LocalContext.current

    // Fetch vaccines from Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("vaccine")
            .get()
            .addOnSuccessListener { result ->
                vaccines.clear()
                for (document in result) {
                    val vaccine = document.toObject(Vaccine::class.java)
                    vaccines.add(vaccine)
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error getting vaccines: $exception"
                showErrorDialog = true
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vaccine Dropdown
        var expanded by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { expanded = true }) {
                Text(selectedVaccine ?: "Select Vaccine")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                vaccines.forEach { vaccine ->
                    DropdownMenuItem(
                        text = { Text(vaccine.name) },
                        onClick = {
                            selectedVaccine = vaccine.name
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date & Time Picker
        Button(onClick = {
            showDateTimePicker(context) { timestamp ->
                selectedDate = timestamp
            }
        }) {
            Text(selectedDate?.toDate().toString() ?: "Select Date & Time")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Make Appointment Button
        Button(
            onClick = {
                if (userId == null || selectedVaccine == null || selectedDate == null) {
                    errorMessage = "Please select a vaccine and date."
                    showErrorDialog = true
                    return@Button
                }

                coroutineScope.launch {
                    val success = userMakesAppointment(userId, selectedVaccine!!, selectedDate!!)
                    if (!success) {
                        errorMessage = "Failed to create appointment."
                        showErrorDialog = true
                    } else {
                        sendNotification(
                            context,
                            title = "Vaccination Appointment",
                            message = "You have made an appointment for $selectedVaccine on ${selectedDate?.toDate()}"
                        )

                        val dayBefore = Calendar.getInstance().apply {
                            time = selectedDate?.toDate() ?: return@apply
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        scheduleNotification(
                            context,
                            dayBefore.timeInMillis,
                            title = "Vaccination Reminder",
                            message = "Your appointment for $selectedVaccine is tomorrow."
                        )


                        //TODO: Move this somewhere - there will be possibility to view all appointments and click on them to add them to calendar
                        addAppointmentToCalendar(
                            context,
                            title = "Vaccination",
                            description = "Vaccination appointment for $selectedVaccine",
                            startTime = selectedDate!!.toDate().time,
                            endTime = selectedDate!!.toDate().time + 60 * 60 * 1000
                        )


                    }
                }
            }
        ) {
            Text("Make Appointment")
        }

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// Function to show Date and Time Picker

fun showDateTimePicker(context: Context, onDateSelected: (Timestamp) -> Unit) {
    val calendar = Calendar.getInstance()
    val datePicker = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val timePicker = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                    }
                    if (selectedCalendar.timeInMillis > System.currentTimeMillis()) {
                        onDateSelected(Timestamp(selectedCalendar.time))
                    } else {
                        Toast.makeText(context, "Select a future date.", Toast.LENGTH_SHORT).show()
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.datePicker.minDate = System.currentTimeMillis() // Ensure only future dates
    datePicker.show()
}





@Composable
fun CertificatesScreen() {
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCertificate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            isLoading = true
            errorMessage = null
            userRepository.fetchUserData(userId) { fetchedUser ->
                if (fetchedUser != null) {
                    userData = fetchedUser
                } else {
                    errorMessage = "Failed to fetch user data."
                }
                isLoading = false
            }
        } ?: run {
            errorMessage = "No user logged in."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Certificates", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Text(text = "Loading...")
            errorMessage != null -> Text(text = errorMessage ?: "An unknown error occurred.")
            userData != null -> {
                if (userData?.vaccinationRecords.isNullOrEmpty()) {
                    Text(text = "No certificates available.")
                } else {
                    LazyColumn {
                        items(userData!!.vaccinationRecords) { certificate ->
                            Button(
                                onClick = {
                                    selectedCertificate = certificate

                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(text = "View Certificate: ${certificate}")
                            }
                        }
                    }
                }
            }
            else -> Text(text = "No data available.")
        }

        selectedCertificate?.let { certificate ->
            Spacer(modifier = Modifier.height(16.dp))
            QRCodeView(certificate)
        }
    }
}

@Composable
fun QRCodeView(qrCodeData: String) {
    val qrBitmap = remember { generateQRCodeBitmap(qrCodeData) }
    qrBitmap?.let {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}


fun generateQRCodeBitmap(data: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Int.ordinalSuffix(): String {
    return when (this % 10) {
        1 -> "${this}st"
        2 -> "${this}nd"
        3 -> "${this}rd"
        else -> "${this}th"
    }
}