package com.example.vaccinetracker.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vaccinetracker.collections.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.DateTime
import deleteAppointment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import loadAppointments
import userMakesVaccination
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdminMainScreen()
        }
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen() {
    val appointments = remember { mutableStateListOf<Appointment>() }
    val visibilityMap = remember { mutableStateMapOf<String, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    // Load appointments on first render
    LaunchedEffect(Unit) {
        val fetchedAppointments = loadAppointments()
        appointments.addAll(fetchedAppointments)
        fetchedAppointments.forEach { visibilityMap[it.appointmentId] = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                actions = {
                    LogoutButtonForAdmin()  // Добавили кнопку выхода здесь
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(appointments) { appointment ->
                val appointmentDate = appointment.getLocalDateTime()

                if (appointmentDate.isBefore(LocalDateTime.now())) {

                val isVisible = visibilityMap[appointment.appointmentId] ?: true

                AnimatedVisibility(
                    visible = isVisible,
                    exit = slideOutVertically() + fadeOut()
                ) {
                    AppointmentItem(
                        appointment,
                        onConfirm = {
                            coroutineScope.launch {
                                val success = userMakesVaccination(
                                    appointment.userId,
                                    appointment.vaccineId,
                                    appointment.getLocalDateTime().toString(),
                                    1
                                )
                                if (success) {
                                    deleteAppointment(appointment.appointmentId)
                                    visibilityMap[appointment.appointmentId] = false
                                    removeWithDelay(appointments, appointment, visibilityMap)
                                } else {
                                    println("User has already made the vaccine")
                                }
                            }
                        },
                        onReject = {
                            deleteAppointment(appointment.appointmentId)
                            visibilityMap[appointment.appointmentId] = false
                            coroutineScope.launch {
                                removeWithDelay(appointments, appointment, visibilityMap)
                            }
                        }
                    )
                }
            }}
        }
    }
}

@Composable
fun LogoutButtonForAdmin() {
    val context = LocalContext.current

    IconButton(onClick = {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, AdminLogInActivity::class.java)
        context.startActivity(intent)

        // Optionally, finish the current activity to prevent back navigation
        if (context is AccountActivity) {
            context.finish()
        }
    }) {
        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
    }
}

// Remove appointment from list with delay
suspend fun removeWithDelay(
    list: SnapshotStateList<Appointment>,
    item: Appointment,
    visibilityMap: MutableMap<String, Boolean>
) {
    delay(300)
    list.removeIf { it.appointmentId == item.appointmentId }
    visibilityMap.remove(item.appointmentId)
}

@Composable
fun AppointmentItem(appointment: Appointment, onConfirm: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "User ID: ${appointment.userId}")
            Text(text = "Vaccine ID: ${appointment.vaccineId}")
            Text(text = "Date: ${appointment.getLocalDateTime()}")
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onConfirm, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text("Confirm")
                }
                Button(onClick = onReject, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("Reject")
                }
            }
        }
    }
}

fun hello() {
    println("Checking overdue appointments...")
}



fun reject(appointment: Appointment) {
    println("Rejected vaccination for User ID: ${appointment.userId}")
}

// Function to generate random appointments


/*// Helper function to generate random DateTime objects
@SuppressLint("NewApi")
fun generateRandomDate(): DateTime {
    val randomEpochSeconds = Instant.now().epochSecond + Random.nextLong(-5 * 24 * 3600, 5 * 24 * 3600)
    return DateTime.newBuilder()
        .setSeconds(randomEpochSeconds.toInt())
        .setNanos(0)
        .build()
}*/

// Extension function to convert Google DateTime to LocalDateTime
// Extension function to convert Firestore timestamp (Long) to LocalDateTime

