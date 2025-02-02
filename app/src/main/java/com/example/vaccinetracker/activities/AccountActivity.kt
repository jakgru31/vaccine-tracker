package com.example.vaccinetracker.activities

import addVaccinesToFirestore
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
//import android.graphics.Color
import androidx.compose.ui.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.google.firebase.firestore.QuerySnapshot
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import userMakesAppointment
import userMakesVaccination
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
//import androidx.compose.ui.graphics.Color // Add this import
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import java.util.*
// Ensure this import is present

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

        // Add logout button here
        LogoutButton()

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Text(text = "Loading...")
            errorMessage != null -> Text(text = errorMessage ?: "An unknown error occurred.")
            userData != null -> {
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
fun LogoutButton() {
    val context = LocalContext.current

    IconButton(onClick = {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)

        // Optionally, finish the current activity to prevent back navigation
        if (context is AccountActivity) {
            context.finish()
        }
    }) {
        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
    }
}


@Composable
fun VaccinesScreen(coroutineScope: CoroutineScope) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedVaccine by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<Timestamp?>(null) }
    val vaccines = remember { mutableStateListOf<Vaccine>() }
    var showVaccineMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val listState = rememberLazyListState()

    // Fetch vaccines from Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("vaccines")
            .get()
            .addOnSuccessListener { result ->
                vaccines.clear()
                for (document in result) {
                    try {
                        val vaccine = document.toObject(Vaccine::class.java)
                        vaccines.add(vaccine)
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error parsing document: ${document.id}", e)
                    }
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error getting vaccines: ${exception.localizedMessage}"
                showErrorDialog = true
            }
    }

    // Scroll down when a vaccine is selected
    LaunchedEffect(selectedVaccine) {
        if (selectedVaccine != null) {
            coroutineScope.launch {
                listState.animateScrollToItem(index = listState.layoutInfo.totalItemsCount - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Schedule Your Vaccination",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showVaccineMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = selectedVaccine ?: "Select Vaccine")
            }

            DropdownMenu(
                expanded = showVaccineMenu,
                onDismissRequest = { showVaccineMenu = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp) // Enables internal scrolling
            ) {
                Column {
                    vaccines.forEach { vaccine ->
                        DropdownMenuItem(
                            text = { Text(vaccine.name) },
                            onClick = {
                                selectedVaccine = vaccine.name
                                showVaccineMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            showDateTimePicker(context) { timestamp ->
                selectedDate = timestamp
            }
        }) {
            Text(
                text = selectedDate?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                } ?: "Click to Choose Date for Appointment"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedVaccine.isNullOrEmpty() || selectedDate == null) {
                    errorMessage = "Please select a vaccine and date."
                    showErrorDialog = true
                    return@Button
                }

                coroutineScope.launch {
                    val success = userMakesAppointment(userId!!, selectedVaccine!!, selectedDate!!)
                    errorMessage = if (!success) {
                        "You already have this vaccine or an existing appointment."
                    } else {
                        "You have successfully booked an appointment for $selectedVaccine on ${selectedDate?.toDate()}"
                    }
                    showErrorDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("Make Appointment", color = Color.White)
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Status") },
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
    var certificateForQrView by remember { mutableStateOf<String?>(null) }
    var qrCodeVisible by remember { mutableStateOf(false) }

    // Fetch user data
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (certificateForQrView != null) {
            // Trigger animation when qrCodeVisible changes
            LaunchedEffect(certificateForQrView) {
                qrCodeVisible = true // Start animation when certificateForQrView is set
            }

            // Fullscreen QR code box with 3D rotation animation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black.copy(alpha = 0.8f))
                    .clickable {
                        certificateForQrView = null
                        qrCodeVisible = false // Reset visibility for next animation
                    },
                contentAlignment = Alignment.Center
            ) {
                // 3D rotation effect on X, Y, and Z axes
                val rotationX by animateFloatAsState(
                    targetValue = if (qrCodeVisible) 360f else 0f,
                    animationSpec = tween(durationMillis = 800)
                )

                val rotationY by animateFloatAsState(
                    targetValue = if (qrCodeVisible) 360f else 0f,
                    animationSpec = tween(durationMillis = 800)
                )

                val rotationZ by animateFloatAsState(
                    targetValue = if (qrCodeVisible) 360f else 0f,
                    animationSpec = tween(durationMillis = 800)
                )

                QRCodeView(
                    qrCodeData = certificateForQrView!!,
                    modifier = Modifier
                        .fillMaxSize()      // Fill the entire screen in fullscreen
                        .aspectRatio(1f)    // Maintain square aspect ratio (important for QR)
                        .graphicsLayer(
                            rotationX = rotationX, // Rotate on X-axis
                            rotationY = rotationY, // Rotate on Y-axis
                            rotationZ = rotationZ  // Rotate on Z-axis (default for flat 2D rotation)
                        )
                )
            }
        } else {
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
                                items(userData!!.vaccinationRecords) { vac_rec_id ->
                                    CertificateItem(
                                        vac_rec_id = vac_rec_id,
                                        onClick = {
                                            certificateForQrView = vac_rec_id // Show QR code for this record
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> Text(text = "No data available.")
                }
            }
        }
    }
}

@Composable
fun CertificateItem(
    vac_rec_id: String,
    onClick: () -> Unit
) {
    var vaccineInfo by remember { mutableStateOf<String?>(null) }

    // Fetch vaccine data when the certificate is clicked
    LaunchedEffect(vac_rec_id) {
        vaccineInfo = getInfo(vac_rec_id) // Fetch vaccine info asynchronously
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // When clicked, show QR code
            .padding(vertical = 8.dp)
    ) {
        // Displaying the vaccine info
        if (vaccineInfo != null) {
            Text(text = "View Certificate: $vaccineInfo")
        } else {
            Text(text = "Loading certificate info...")
        }
    }
}

suspend fun getInfo(vac_rec_id: String): String? {
    val db = FirebaseFirestore.getInstance()
    val result = db.collection("vaccination_records")
        .whereEqualTo("vaccinationRecordUid", vac_rec_id)
        .get()
        .await()

    return result.documents.firstOrNull()?.getString("vaccineUid") ?: "Unknown vaccine"
}


@Composable
fun QRCodeView(qrCodeData: String, modifier: Modifier = Modifier) {
    val qrBitmap = remember { generateQRCodeBitmap(qrCodeData) }
    qrBitmap?.let {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(300.dp) // Adjust the size of the QR code
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
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
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