package com.example.vaccinetracker.activities

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
import com.example.vaccinetracker.adapters.CertificateAdapter
import com.example.vaccinetracker.adapters.VaccinationHistoryAdapter
import com.example.vaccinetracker.data.Certificate
import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.data.VaccinationHistory
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

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
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") { HomeScreen() }
        composable("vaccines") { VaccinesScreen() }
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
    val user = FirebaseAuth.getInstance().currentUser
    var userData by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { userId ->
            User.fetchUserData(userId) { fetchedUser ->
                userData = fetchedUser
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(16.dp))

        user?.let {
            if (userData != null) {
                Text(text = "Welcome, ${userData?.name ?: it.email}")
                Spacer(modifier = Modifier.padding(8.dp))
                Text(text = "Vaccination Status: Verified")
            } else {
                Text(text = "Loading user data...")
            }
        } ?: Text(text = "No user details available.")
    }
}

@Composable
fun VaccinesScreen() {
    // Create an instance of the adapter for fetching vaccination history
    val vaccinationHistoryAdapter = remember { VaccinationHistoryAdapter() }

    // Mutable state list to store vaccination history and observe changes
    val vaccinationHistory = remember { mutableStateListOf<VaccinationHistory>() }

    // Get the current user's ID from Firebase Authentication
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Fetch vaccination history when the user ID becomes available
    LaunchedEffect(userId) {
        userId?.let {
            // Call adapter's function to fetch vaccination history
            vaccinationHistoryAdapter.fetchVaccinationHistoryFromFirebase(it) { fetchedHistory ->
                vaccinationHistory.clear() // Clear any existing records
                vaccinationHistory.addAll(fetchedHistory) // Add fetched records to the list
            }
        }
    }

    // Render the UI elements for displaying vaccination history
    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the available screen space
            .padding(16.dp) // Add padding around the column
    ) {
        // Display the title of the screen
        Text(text = "Vaccines", style = MaterialTheme.typography.headlineMedium)

        // Add vertical spacing below the title
        Spacer(modifier = Modifier.height(16.dp))

        // Display a scrollable list of vaccination history
        LazyColumn {
            // Iterate through the vaccination history list and display each record
            items(vaccinationHistory) { record ->
                // Display details of each vaccine record
                Text(
                    text = "${record.vaccine.name}: ${record.doseNumber.ordinalSuffix()} Dose - ${record.dateAdministered}"
                )
                // Add spacing between records
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun CertificatesScreen() {
    // Create an instance of the adapter for fetching certificates from Firebase
    val certificateAdapter = remember { CertificateAdapter() }

    // Mutable state list to hold certificates and observe changes for recomposition
    val certificates = remember { mutableStateListOf<Certificate>() }

    // Fetch certificates from Firebase when this composable is first launched
    LaunchedEffect(Unit) {
        // Call adapter's function to fetch certificates from Firebase
        certificateAdapter.fetchCertificatesFromFirebase { fetchedCertificates ->
            certificates.clear() // Clear any existing certificates
            certificates.addAll(fetchedCertificates) // Add fetched certificates to the list
        }
    }

    // Render the UI elements for displaying certificates
    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the available screen space
            .padding(16.dp) // Add padding around the column
    ) {
        // Display the title of the screen
        Text(text = "Certificates", style = MaterialTheme.typography.headlineMedium)

        // Add vertical spacing below the title
        Spacer(modifier = Modifier.height(16.dp))

        // Display a scrollable list of certificates
        LazyColumn {
            // Iterate through the certificates list and display each certificate
            items(certificates) { certificate ->
                // Button to represent each certificate
                Button(
                    onClick = { /* Handle certificate selection */ }, // Action when clicked
                    modifier = Modifier.padding(vertical = 8.dp) // Add vertical padding around the button
                ) {
                    // Display the vaccine name on the button
                    Text(text = "View Certificate: ${certificate.vaccineName}")
                }
            }
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
