import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.data.VaccinationRecord
import com.example.vaccinetracker.data.VaccineAppointment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID


// ...

suspend fun addNewUserToDatabase(user: User): Boolean { // Return success/failure
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    try {
        val userCredential = auth.createUserWithEmailAndPassword(user.email, user.password).await()
        val firebaseUser: FirebaseUser = userCredential.user!!
        val uid = firebaseUser.uid

        user.id = uid
        db.collection("users").document(uid).set(user).await()

        println("User added successfully")
        return true // Success

    } catch (e: Exception) {
        val errorMessage = when (e) {
            is com.google.firebase.firestore.FirebaseFirestoreException -> {
                when (e.code) {
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "Document already exists."
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ALREADY_EXISTS -> "User with this email already exists." // More specific
                    else -> "Firestore error: ${e.message}"
                }
            }
            is FirebaseAuthException -> { // Handle FirebaseAuth exceptions
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email format."
                    "ERROR_WEAK_PASSWORD" -> "Password too weak."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
                    else -> "Authentication error: ${e.message}"
                }
            }

            else -> "Error adding user: ${e.message}"

        }
        println("Error adding user: $errorMessage")
        return false // Failure
    }
}

suspend fun userMakesVaccination2(userUid: String, vaccineUid: String, dateAdministered: String, doseNumber: Int)
{

}


suspend fun updateUserData(userUid: String, )
{
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

}


// A vaccination Record is created and stored as well as its id in the users vaccination records list
suspend fun userMakesVaccination(userUid: String, vaccineUid: String, dateAdministered: String, doseNumber: Int): Boolean {
    val db = FirebaseFirestore.getInstance()

    println("userUid: $userUid, vaccineUid: $vaccineUid, dateAdministered: $dateAdministered, doseNumber: $doseNumber")
    return withContext(Dispatchers.IO) {
        try {
            // Generate a new UID for the vaccination record
            val vaccinationRecordUid = UUID.randomUUID().toString()
            val newVaccinationRecord = VaccinationRecord(
                vaccinationRecordUid, userUid, vaccineUid, dateAdministered, doseNumber
            )

            val userDocRef = db.collection("users").document(userUid)
            val vaccinationRecordRef = db.collection("vaccination_records").document(vaccinationRecordUid)

            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDocRef)

                if (!userSnapshot.exists()) {
                    throw FirebaseFirestoreException("User not found", FirebaseFirestoreException.Code.NOT_FOUND)
                }

                val user = userSnapshot.toObject(User::class.java)
                    ?: throw FirebaseFirestoreException("User conversion error", FirebaseFirestoreException.Code.FAILED_PRECONDITION)

                // Update user's vaccination records list (store only UID)
                val updatedVaccinationRecords = user.vaccinationRecords.toMutableList()
                updatedVaccinationRecords.add(vaccinationRecordUid)

                // Save the vaccination record
                transaction.set(vaccinationRecordRef, newVaccinationRecord)

                // Update user's vaccination records list
                transaction.update(userDocRef, "vaccinationRecords", updatedVaccinationRecords)
            }.await()

            true // Transaction was successful
        } catch (e: Exception) {
            println("Error adding vaccination record: ${e.message}")
            false // Transaction failed
        }
    }
}
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

/*suspend fun addVaccinationHistory(history: VaccinationHistory) {
    val db = FirebaseFirestore.getInstance()
    val historyRef = db.collection("vaccinationHistory").document(history.historyId)

    try {
        historyRef.set(history).await()
        println("Vaccination history added successfully")
    } catch (e: Exception) {
        println("Error adding vaccination history: $e")
    }
}*/

