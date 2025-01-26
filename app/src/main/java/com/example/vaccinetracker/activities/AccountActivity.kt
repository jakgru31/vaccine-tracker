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
import com.example.vaccinetracker.data.UserRepository
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
                if (userData?.vaccinationHistories.isNullOrEmpty()) {
                    Text(text = "No certificates available.")
                } else {
                    userData?.vaccinationHistories?.forEach { certificate ->
                        Text(text = "- ${certificate.vaccineUid}")
                    }
                }
            }
            else -> Text(text = "No data available.")
        }
    }
}

@Composable
fun VaccinesScreen() {
    val vaccinationHistoryAdapter = remember { VaccinationHistoryAdapter() }
    val vaccinationHistory = remember { mutableStateListOf<VaccinationHistory>() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        userId?.let {
            vaccinationHistoryAdapter.fetchVaccinationHistoryFromFirebase(it) { fetchedHistory ->
                vaccinationHistory.clear()
                vaccinationHistory.addAll(fetchedHistory)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Vaccines", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(vaccinationHistory) { record ->
                Text(
                    text = "${record.vaccineUid}: ${record.doseNumber.ordinalSuffix()} Dose - ${record.dateAdministered}"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CertificatesScreen() {
    val certificateAdapter = remember { CertificateAdapter() }
    val certificates = remember { mutableStateListOf<Certificate>() }

    LaunchedEffect(Unit) {
        certificateAdapter.fetchCertificatesFromFirebase { fetchedCertificates ->
            certificates.clear()
            certificates.addAll(fetchedCertificates)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Certificates", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(certificates) { certificate ->
                Button(
                    onClick = { /* Handle certificate selection */ },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
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
