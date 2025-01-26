import com.example.vaccinetracker.data.User
import com.example.vaccinetracker.data.VaccinationHistory
import com.example.vaccinetracker.data.VaccineAppointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await


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

suspend fun userMakesVaccination(userId: String, vaccineId: String)
{

}


suspend fun updateUserData(userUid: String, )
{
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

}


/*
suspend fun addVaccinationRecord(
    userUid: String,
    vaccineUid: String,
    dateAdministered: String,
    doseNumber: Int
): Result<Unit> {
    val db = FirebaseFirestore.getInstance()
    return try {
        val newVaccinationHistory = VaccinationHistory(vaccinationHistoryUid = "", userUid = userUid, vaccineUid = vaccineUid, dateAdministered = dateAdministered, doseNumber = doseNumber)


        val userDocRef = db.collection("users").document(userUid)

        val userSnapshot = userDocRef.get().await()

        if (userSnapshot.exists()) {
            val user = userSnapshot.toObject(User::class.java) ?: return Result.failure(Exception("Error retrieving User object"))


            // Crucial:  Update the existing list, rather than just adding.
            val updatedVaccinationHistories = user.vaccinationHistories + newVaccinationHistory

            val updatedUserData = mapOf("vaccinationHistories" to updatedVaccinationHistories)
            val updateResult = userDocRef.set(updatedUserData, SetOptions.merge()).await() // Using set


            if (updateResult.isSuccessful) {
                return Result.success(Unit)
            } else {
                return Result.failure(Exception("Error updating vaccinationHistory"))
            }
        } else {
            return Result.failure(Exception("User document not found for $userUid"))
        }

    } catch (e: Exception) {
        return Result.failure(e)
    }
}
*/

/*suspend fun fetchUserData(userId: String): User? {
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
}*/



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

