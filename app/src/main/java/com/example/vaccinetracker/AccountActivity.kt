package com.example.vaccinetracker

//import androidx.compose.material.icons.Default.Description
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import VaccinationHistory
import Vaccine
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
//import androidx.test.espresso.base.Default
import com.example.vaccinetracker.ui.theme.VaccineTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import fetchUserData

// The main entry point for the app
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(user?.uid) {
        user?.uid.let{ userId ->
            if (userId != null) {
                userData = fetchUserData(userId)
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
                Text(text = "Welcome, ${userData?.name ?: it.email}")  // Display name if available, else email
                Spacer(modifier = Modifier.padding(8.dp))
                Text(text = "Vaccination Status: Verified") // Placeholder text
            } else {
                Text(text = "Loading user data...")
            }
        } ?: Text(text = "No user details available.")
    }
}

@Composable
fun VaccinesScreen() {  
    val vaccinationHistory = remember { mutableStateListOf<VaccinationHistory>() }

    LaunchedEffect(Unit) {
        val pfizerVaccine = Vaccine(
            vaccineId = "vaccine123",
            name = "Pfizer-BioNTech Covid-19 Vaccine",
            manufacturer = "Pfizer, Inc.",
            type = "mRNA",
            dosesRequired = 2,
            recommendedInterval = 21,
            commonSideEffects = listOf("Fever", "Fatigue", "Headache", "Pain at injection site")
        )

        vaccinationHistory.addAll(
            listOf(
                VaccinationHistory("1", "user123", pfizerVaccine, "Dec 16, 2020", 1),
                VaccinationHistory("2", "user123", pfizerVaccine, "Jan 6, 2021", 2)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Vaccines", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        vaccinationHistory.forEach { record ->
            Text(
                text = "${record.vaccine.name}: ${record.doseNumber.ordinalSuffix()} Dose - ${record.dateAdministered}"
            )
        }
    }
}

@Composable
fun CertificatesScreen() {
    val certificates = remember { mutableStateListOf<Certificate>() }
    val selectedCertificate = remember { mutableStateOf<Certificate?>(null) }

    LaunchedEffect(Unit) {
        certificates.addAll(
            listOf(
                Certificate(
                    certificateId = "cert1",
                    userId = "user123",
                    vaccineName = "Pfizer-BioNTech Covid-19 Vaccine",
                    dateAdministered = "Jan 6, 2021",
                    doseNumber = 2,
                    qrCodeData = "https://example.com/certificate/cert1"
                ),
                Certificate(
                    certificateId = "cert2",
                    userId = "user123",
                    vaccineName = "Influenza Vaccine",
                    dateAdministered = "Nov 4, 2019",
                    doseNumber = 1,
                    qrCodeData = "https://example.com/certificate/cert2"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Certificates", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        certificates.forEach { certificate ->
            Button(
                onClick = { selectedCertificate.value = certificate },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(text = "View Certificate: ${certificate.vaccineName}")
            }
        }

        selectedCertificate.value?.let { certificate ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "QR Code for: ${certificate.vaccineName}")
            Spacer(modifier = Modifier.height(8.dp))
            QRCodeView(qrCodeData = certificate.qrCodeData)
        }
    }
}

@Composable
fun QRCodeView(qrCodeData: String) {
    val qrBitmap = remember { generateQRCodeBitmap(qrCodeData) }
    qrBitmap?.let {
        Box(
            modifier = Modifier.fillMaxWidth(), // Makes Box take full width for proper alignment
            contentAlignment = Alignment.Center // Centers content inside the Box
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(200.dp) // Set size of the QR code
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
