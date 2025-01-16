import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.data.VaccinationHistory
import com.example.vaccinetracker.data.VaccineAppointment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


/*//suspend fun addUserToDatabase(user: User) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(user.id)

    try {
        // Use 'await()' to wait for the result of the operation
        userRef.set(user).await()
        println("User added successfully")
    } catch (e: Exception) {
        println("Error adding user: $e")
    }
}*/

suspend fun fetchUserData(userId: String): User? {
    val db = FirebaseFirestore.getInstance()
    return try {
        println("Attempting to fetch user data")
        val document = db.collection("users").document(userId).get().await()
        if (!document.exists()) {
            println("Document not found")
            return null
        }
        document.toObject(User::class.java)?.also {
            println("User data fetched successfully")
        }
    } catch (e: Exception) {
        println("Error fetching user data: $e")
        null
    }
}



suspend fun addVaccineAppointment(appointment: VaccineAppointment) {
    val db = FirebaseFirestore.getInstance()
    val appointmentRef = db.collection("appointments").document(appointment.appointmentId)

    try {
        appointmentRef.set(appointment).await()
        println("Appointment added successfully")
    } catch (e: Exception) {
        println("Error adding appointment: $e")
    }
}


suspend fun getAppointmentsForUser(userId: String): List<VaccineAppointment> {
    val db = FirebaseFirestore.getInstance()
    val appointments = mutableListOf<VaccineAppointment>()

    try {
        val result = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .get()
            .await()  // Await the result of the query
        for (document in result) {
            val appointment = document.toObject(VaccineAppointment::class.java)
            appointments.add(appointment)
        }
    } catch (e: Exception) {
        println("Error getting appointments: $e")
    }

    return appointments
}


suspend fun updateAppointmentStatus(appointmentId: String, status: String) {
    val db = FirebaseFirestore.getInstance()
    val appointmentRef = db.collection("appointments").document(appointmentId)

    try {
        appointmentRef.update("status", status).await()
        println("Appointment status updated")
    } catch (e: Exception) {
        println("Error updating appointment status: $e")
    }
}

suspend fun addVaccinationHistory(history: VaccinationHistory) {
    val db = FirebaseFirestore.getInstance()
    val historyRef = db.collection("vaccinationHistory").document(history.historyId)

    try {
        historyRef.set(history).await()
        println("Vaccination history added successfully")
    } catch (e: Exception) {
        println("Error adding vaccination history: $e")
    }
}

