package com.example.vaccinetracker.activities

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.Add
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
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.vaccinetracker.collections.Appointment
import generateChatBotSuggestions
import loadAppointmentsForOneUser
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// Main activity for the Account screen
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

// Main composable screen that includes top app bar, bottom navigation, and content navigation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Vaccine Tracker") },
                actions = {
                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)

                        if (context is AccountActivity) {
                            context.finish()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log out")
                    }

                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false }
        ) {
            SettingsScreen()
        }
    }
}

// Composable to handle navigation between different screens
@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier) {
    val coroutineScope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }
        composable("vaccines") { VaccinesScreen(coroutineScope = coroutineScope)}
        composable("certificates") { CertificatesScreen() }
        composable("settings") { SettingsScreen() }
    }
}

// Composable for the bottom navigation bar with different items
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
                        BottomNavItem.Vaccines -> Icon(Icons.Default.Menu, contentDescription = "Vaccines")
                        BottomNavItem.Certificates -> Icon(Icons.Default.Check, contentDescription = "Certificates")
                    }
                },
                label = { Text(text = item.title) }
            )
        }
    }
}

// Sealed class to define different bottom navigation items
sealed class BottomNavItem(val route: String, val title: String) {
    object Home : BottomNavItem("home", "Home")
    object Vaccines : BottomNavItem("vaccines", "Vaccines")
    object Certificates : BottomNavItem("certificates", "Certificates")
}

// Composable for the home screen displaying user data and appointments
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }
    var userErrorMessage by remember { mutableStateOf<String?>(null) }

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoadingAppointments by remember { mutableStateOf(true) }
    var appointmentErrorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Load user data from Firestore
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            isLoadingUser = true
            userErrorMessage = null
            userRepository.fetchUserData(userId) { fetchedUser ->
                if (fetchedUser != null) {
                    userData = fetchedUser
                } else {
                    userErrorMessage = "Failed to fetch user data."
                }
                isLoadingUser = false
            }
        } ?: run {
            userErrorMessage = "No user logged in."
            isLoadingUser = false
        }
    }

    // Load appointments for the current user
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            isLoadingAppointments = true
            appointmentErrorMessage = null
            try {
                appointments = loadAppointmentsForOneUser(userId)
            } catch (e: Exception) {
                appointmentErrorMessage = "Failed to load appointments: ${e.message}"
            } finally {
                isLoadingAppointments = false
            }
        }
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item{
                // Header section displaying user's name
                Text(
                    text = "Hello, ${userData?.name ?: "User"}!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar section to display appointments
                CalendarWidget(appointments)

                Spacer(modifier = Modifier.height(24.dp))

                // Reminders section listing upcoming appointments
                Text(
                    text = "Reminders",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Conditional rendering of appointments based on loading and error states
            when {
                isLoadingAppointments -> item{Text(text = "Loading appointments...")}
                appointmentErrorMessage != null -> item { Text(text = appointmentErrorMessage!!) }
                appointments.isEmpty() -> item { Text(text = "No appointments found.") }
                else -> {
                    items(appointments) { appointment ->
                        ReminderItem(
                            title = appointment.vaccineId,
                            date = appointment.appointmentDate.toDate().toString()
                        )
                    }
                }
            }
        }
    }
}

// Composable to display a single reminder item
@Composable
fun ReminderItem(title: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Calendar Icon",
            modifier = Modifier
                .size(45.dp)
                .padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

// Composable for a log out button
@Composable
fun LogoutButton() {
    val context = LocalContext.current

    // Button to log out with text and icon
    Button(
        onClick = {
            // Sign out logic
            FirebaseAuth.getInstance().signOut()

            // Redirect to login screen
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)

            // Optionally, finish the current activity to prevent back navigation
            if (context is AccountActivity) {
                context.finish()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF007BFF), // Customize color
            contentColor = Color.White
        ),
        modifier = Modifier
            .padding(16.dp) // Adds padding from the edges of the parent container
            .sizeIn(minWidth = 120.dp, minHeight = 50.dp) // Defines minimum size for the button
    ) {
        Icon(Icons.Default.ExitToApp, contentDescription = "Log out")
        Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text
        Text(text = "Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp) // Text next to the icon
    }
}

// Composable for displaying a calendar widget with appointments
@Composable
fun CalendarWidget(appointments: List<Appointment>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentDate = remember { LocalDate.now() }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
    val selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    // Extract appointment days for the current month
    val appointmentDays = appointments.mapNotNull { vaccine ->
        vaccine.appointmentDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .takeIf { it.year == currentMonth.year && it.month == currentMonth.month }
    }.map { it.dayOfMonth }

    // Box to contain the calendar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .border(1.dp, colorScheme.onSurface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row to navigate between months
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentMonth.year}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }

            // Canvas to draw the calendar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val dayWidth = size.width / 7
                            val dayHeight = size.height / 6
                            val column = (offset.x / dayWidth).toInt()
                            val row = (offset.y / dayHeight).toInt()
                            val day = row * 7 + column - firstDayOfMonth + 1
                            if (day in 1..daysInMonth) {
                                selectedDate.value = currentMonth.atDay(day)
                            }
                        }
                    }
            ) {
                val dayWidth = size.width / 7
                val dayHeight = size.height / 6

                // Loop through days of the month to draw them
                for (day in 1..daysInMonth) {
                    val column = (day + firstDayOfMonth - 1) % 7
                    val row = (day + firstDayOfMonth - 1) / 7
                    val x = column * dayWidth + dayWidth / 2
                    val y = row * dayHeight + dayHeight / 2

                    // Draw circle highlight for appointment days
                    if (day in appointmentDays) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = minOf(dayWidth, dayHeight) / 3,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }

                    // Draw day number
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            day.toString(),
                            x,
                            y + 15f, // Adjust vertical position slightly for alignment
                            android.graphics.Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 40f
                                color = colorScheme.onSurface.toArgb()
                            }
                        )
                    }
                }
            }
        }
    }
}

// Composable for the vaccines screen where users can schedule vaccinations
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
    var userQuestion by remember { mutableStateOf("") }
    var chatbotResponse by remember { mutableStateOf("") }

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

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header for the screen
        item {
            Text(
                text = "Schedule Your Vaccination",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Dropdown menu to select a vaccine
        item {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
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

        // Button to select date and time for the appointment
        item {
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
        }

        // Button to make the appointment
        item {
            Button(
                onClick = {
                    if (selectedVaccine.isNullOrEmpty() || selectedDate == null) {
                        errorMessage = "Please select a vaccine and date."
                        showErrorDialog = true
                        return@Button
                    }

                    coroutineScope.launch {
                        val success = userMakesAppointment(userId!!, selectedVaccine!!, selectedDate!!)
                        if (success) {
                            sendNotification(
                                context,
                                "Appointment Scheduled",
                                "You scheduled an appointment for ${selectedDate!!.toDate()} for $selectedVaccine vaccination."
                            )
                            val notificationTime = selectedDate!!.toDate().time - 24 * 60 * 60 * 1000
                            scheduleNotification(context, notificationTime, "Reminder", "Your appointment for $selectedVaccine is tomorrow.")
                            errorMessage = "Appointment scheduled successfully."
                            showErrorDialog = true
                        } else {
                            errorMessage = "Failed to schedule appointment."
                            showErrorDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Make Appointment")
            }
        }

        // Outlined text field for user to type a question
        item {
            OutlinedTextField(
                value = userQuestion,
                onValueChange = { userQuestion = it },
                label = { Text("Start a chat with the VaccineTracker AI Assistant: ") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Button to get a response from the chatbot
        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        chatbotResponse = generateChatBotSuggestions(userQuestion)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Answer")
            }
        }

        // Display chatbot response if available
        if (chatbotResponse.isNotEmpty()) {
            item {
                Text("Chatbot: $chatbotResponse", fontWeight = FontWeight.Bold)
            }
        }

        // Display error dialog if there is an error message
        if (showErrorDialog) {
            item {
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
}

// Function to show date and time picker dialog
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

// Composable for the certificates screen displaying user's vaccination certificates
@Composable
fun CertificatesScreen() {
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var certificateForQrView by remember { mutableStateOf<String?>(null) }
    var qrCodeVisible by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Load user data from Firestore
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
        // Check if a certificate is selected for QR code view
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
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .graphicsLayer(
                            rotationX = rotationX,
                            rotationY = rotationY,
                            rotationZ = rotationZ
                        )
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(text = "Certificates", style = MaterialTheme.typography.headlineMedium)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Conditional rendering of certificates based on loading and error states
                when {
                    isLoading -> item { Text(text = "Loading...") }
                    errorMessage != null -> item { Text(text = errorMessage ?: "An unknown error occurred.") }
                    userData != null -> {
                        if (userData?.vaccinationRecords.isNullOrEmpty()) {
                            item { Text(text = "No certificates available.") }
                        } else {
                            items(userData!!.vaccinationRecords) { vac_rec_id ->
                                CertificateItem(
                                    vac_rec_id = vac_rec_id,
                                    onClick = { certificateForQrView = vac_rec_id }
                                )
                            }
                        }
                    }
                    else -> item { Text(text = "No data available.") }
                }
            }
        }
    }
}

// Composable for displaying a single certificate item
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

    // Card to represent a certificate item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // When clicked, show QR code
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = vaccineInfo?.let { "View Certificate: $it" } ?: "Loading certificate info...",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.Black
            )
        }
    }
}

// Suspend function to fetch vaccine information based on the record ID
suspend fun getInfo(vac_rec_id: String): String? {
    val db = FirebaseFirestore.getInstance()
    val result = db.collection("vaccination_records")
        .whereEqualTo("vaccinationRecordUid", vac_rec_id)
        .get()
        .await()

    return result.documents.firstOrNull()?.getString("vaccineUid") ?: "Unknown vaccine"
}

// Composable to display a QR code
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

// Function to generate a QR code bitmap from given data
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

// Composable for the settings screen where users can change their name and password
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var newName by remember { mutableStateOf("") }
    var newSurname by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordConfirm by remember { mutableStateOf("") }

    // States for each item expansion
    var expandedName by remember { mutableStateOf(false) }
    var expandedPassword by remember { mutableStateOf(false) }

    // Column layout for settings items
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Card for changing name
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedName = !expandedName }
                    .padding(16.dp)
            ) {
                Text(text = "Change Name", style = MaterialTheme.typography.bodyLarge)
                if (expandedName) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = newSurname,
                        onValueChange = { newSurname = it },
                        label = { Text("New Surname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (newName.isNotEmpty() && newSurname.isNotEmpty() && nameSurnameValidator(newName, newSurname)) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            if (userId != null) {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(userId)
                                    .update(mapOf("name" to newName, "surname" to newSurname))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Invalid name or surname", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Update Name")
                    }
                }
            }
        }

        // Card for changing password
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedPassword = !expandedPassword }
                    .padding(16.dp)
            ) {
                Text(text = "Change Password", style = MaterialTheme.typography.bodyLarge)
                if (expandedPassword) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = newPasswordConfirm,
                        onValueChange = { newPasswordConfirm = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (newPassword.isNotEmpty() && newPassword == newPasswordConfirm && passwordValidator(newPassword)) {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.updatePassword(newPassword)
                                ?.addOnSuccessListener {
                                    Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                                }
                                ?.addOnFailureListener {
                                    Toast.makeText(context, "Failed to update password", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Invalid password", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Update Password")
                    }
                }
            }
        }
    }
}

// Extension function to add ordinal suffix to an integer
fun Int.ordinalSuffix(): String {
    return when (this % 10) {
        1 -> "${this}st"
        2 -> "${this}nd"
        3 -> "${this}rd"
        else -> "${this}th"
    }
}

// Function to validate the password
private fun passwordValidator(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}$")
    return passwordPattern.matches(password)
}

// Function to validate the name and surname
private fun nameSurnameValidator(name: String, surname: String): Boolean {
    val namePattern = Regex("^[a-zA-Z]+$") // Ensure only letters
    return namePattern.matches(name) && namePattern.matches(surname)
}
