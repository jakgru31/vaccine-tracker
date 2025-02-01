package com.example.vaccinetracker.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vaccinetracker.collections.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.DateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
        topBar = { TopAppBar(title = { Text("Admin Panel") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(appointments) { appointment ->
                val appointmentDate = appointment.getLocalDateTime()

                if (appointmentDate.isBefore(LocalDateTime.now())) {
                    hello()
                }

                val isVisible = visibilityMap[appointment.appointmentId] ?: true

                AnimatedVisibility(
                    visible = isVisible,
                    exit = slideOutVertically() + fadeOut()
                ) {
                    AppointmentItem(
                        appointment,
                        onConfirm = {
                            usv(appointment)
                            visibilityMap[appointment.appointmentId] = false
                            coroutineScope.launch {
                                removeWithDelay(appointments, appointment, visibilityMap)
                            }
                        },
                        onReject = {
                            reject(appointment)
                            visibilityMap[appointment.appointmentId] = false
                            coroutineScope.launch {
                                removeWithDelay(appointments, appointment, visibilityMap)
                            }
                        }
                    )
                }
            }
        }
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

fun usv(appointment: Appointment) {
    println("Confirmed vaccination for User ID: ${appointment.userId}")
}

fun reject(appointment: Appointment) {
    println("Rejected vaccination for User ID: ${appointment.userId}")
}

// Function to generate random appointments
suspend fun loadAppointments(): List<Appointment> {
    val db = FirebaseFirestore.getInstance()
    return try {
        val snapshot = db.collection("appointments").get().await()
        val appointments = snapshot.documents.mapNotNull { it.toObject(Appointment::class.java) }

        // Log retrieved data
        println("Appointments fetched: ${appointments.size}")
        appointments.forEach { println("Appointment: $it") }

        appointments
    } catch (e: Exception) {
        e.printStackTrace()
        println("Error fetching appointments: ${e.message}")
        emptyList()
    }
}

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

