package com.example.vaccinetracker.activities


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
//import androidx.compose.ui.graphics.Color // Add this import
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.vaccinetracker.collections.Appointment
import loadAppointmentsForOneUser
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
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
    val coroutineScope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Vaccine Tracker (Version 0.8)") },
                    actions = {
                        IconButton(
                            onClick = { showSettings = true }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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

    // Load user data
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

    // Load appointments
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome Back!") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)

                        if (context is AccountActivity) {
                            context.finish()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Log out")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item{
            // Header Section
            Text(
                text = "Hello, ${userData?.name ?: "User"}!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Section
            CalendarWidget(appointments)

            Spacer(modifier = Modifier.height(24.dp))

            // Reminders Section
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            }

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
        //.align(Alignment.TopEnd) // Align the button to the top-right corner of its parent container (Scaffold)

    ) {
        Icon(Icons.Default.ExitToApp, contentDescription = "Log out")
        Spacer(modifier = Modifier.width(8.dp)) // Add space between icon and text
        Text(text = "Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp) // Text next to the icon
    }
}
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
                    if (success) {
                        sendNotification(context, "Appointment Scheduled", "You  scheduled an appointment for ${selectedDate!!.toDate()} for $selectedVaccine vaccination.")
                        val notificationTime = selectedDate!!.toDate().time -  24 * 60 * 60 * 1000 // 24 hours before appointment
                        scheduleNotification(context, notificationTime, "Reminder", "Your appointment for $selectedVaccine is tomorrow.")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Change Name Section
        LazyColumn {
            item {
                ListItem(
                    headlineContent = { Text("Change Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedName = !expandedName }
                )
                if (expandedName) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                        Button(onClick = {
                            if (newName.isNotEmpty() && newSurname.isNotEmpty()) {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("users").document(userId)
                                        .update(mapOf("name" to newName, "surname" to newSurname))
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }) {
                            Text("Update Name")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Change Password Section
                ListItem(
                    headlineContent = { Text("Change Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPassword = !expandedPassword }
                )
                if (expandedPassword) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            }
                        }) {
                            Text("Update Password")
                        }
                    }
                }
            }
        }
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

private fun passwordValidator(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}$")
    return passwordPattern.matches(password)
}