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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vaccinetracker.collections.Appointment
import com.google.firebase.auth.FirebaseAuth
import deleteAppointment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import loadAppointments
import userMakesVaccination
import java.time.LocalDateTime

/**
 * AdminActivity class that serves as the main screen for administrators.
 * It initializes the admin panel UI.
 */
class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdminMainScreen()
        }
    }
}

/**
 * Composable function that displays the Admin Panel.
 * It loads and displays a list of appointments, allowing admin actions.
 */
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
                    LogoutButtonForAdmin()
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
                }
            }
        }
    }
}

/**
 * Composable function that creates a logout button for the admin.
 * Logs out the admin and redirects to the login screen.
 */
@Composable
fun LogoutButtonForAdmin() {
    val context = LocalContext.current

    Button(
        onClick = {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(context, AdminLogInActivity::class.java)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF007BFF),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(16.dp).sizeIn(minWidth = 120.dp, minHeight = 50.dp)
    ) {
        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/**
 * Suspends execution for a short duration before removing an appointment.
 * @param list The mutable list of appointments.
 * @param item The appointment to remove.
 * @param visibilityMap The map tracking appointment visibility.
 */
suspend fun removeWithDelay(
    list: SnapshotStateList<Appointment>,
    item: Appointment,
    visibilityMap: MutableMap<String, Boolean>
) {
    delay(300)
    list.removeIf { it.appointmentId == item.appointmentId }
    visibilityMap.remove(item.appointmentId)
}

/**
 * Composable function that displays an individual appointment item.
 * @param appointment The appointment details.
 * @param onConfirm Lambda function executed on confirmation.
 * @param onReject Lambda function executed on rejection.
 */
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
