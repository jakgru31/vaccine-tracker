import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.vaccinetracker.collections.Appointment
import com.example.vaccinetracker.collections.User
import com.example.vaccinetracker.collections.VaccinationRecord
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.type.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import com.google.firebase.firestore.FieldPath

//TODO Works
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

//TODO Works
// A vaccination Record is created and stored as well as its id in the users vaccination records list

suspend fun userMakesVaccination(userUid: String, vaccineUid: String, dateAdministered: String, doseNumber: Int): Boolean {
    val db = FirebaseFirestore.getInstance()

    println("userUid: $userUid, vaccineUid: $vaccineUid, dateAdministered: $dateAdministered, doseNumber: $doseNumber")

    return withContext(Dispatchers.IO) {
        try {
            // Step 1: Check if the vaccineUid already exists for the user
            val vaccinationRecords = db.collection("vaccination_records")
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("vaccineUid", vaccineUid)
                .get()
                .await()

            // If there's already a record with this vaccineUid for the user, return false
            if (!vaccinationRecords.isEmpty) {
                println("User has already received this vaccine.")
                return@withContext false
            }
            else{println(vaccinationRecords.isEmpty)}

            // Step 2: Generate a new UID for the vaccination record
            val vaccinationRecordUid = UUID.randomUUID().toString()
            val newVaccinationRecord = VaccinationRecord(
                vaccinationRecordUid, userUid, vaccineUid, dateAdministered, doseNumber
            )

            val userDocRef = db.collection("users").document(userUid)
            val vaccinationRecordRef = db.collection("vaccination_records").document(vaccinationRecordUid)

            // Step 3: Run the Firestore transaction to add the vaccination record
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



//TODO Works
suspend fun userMakesAppointment(userId: String, vaccineId: String, appointmentDate: Timestamp): Boolean {
    val db = FirebaseFirestore.getInstance()
    val appointmentId = UUID.randomUUID().toString()
    val appointment = Appointment(appointmentId, userId, vaccineId, appointmentDate)

    return withContext(Dispatchers.IO) {
        try {

            val appointments = db.collection("appointments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("vaccineId", vaccineId)
                .get()
                .await()

            // If there's already a record with this vaccineUid for the user, return false
            if (!appointments.isEmpty) {
                println("User has already received this vaccine.")
                return@withContext false
            }
            else{println(appointments.isEmpty)}

            db.collection("appointments").document(appointmentId).set(appointment).await()
            println("Appointment added successfully")
            true
        } catch (e: Exception) {
            println("Error processing appointment: ${e.message}")
            false
        }
    }
}


//TODO needs to be checked
fun deleteAppointment(docId: String) {

    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("appointments").document(docId)

    docRef.delete()
        .addOnSuccessListener {
            // Document was successfully deleted
            println("Document successfully deleted!")
        }
        .addOnFailureListener { e ->
            // If an error occurs
            println("Error deleting document: $e")
        }
}


// Suspend function to load appointments from Firestore
/*suspend fun loadAppointments(): SnapshotStateList<Appointment> {
    val db = FirebaseFirestore.getInstance()
    val collectionRef = db.collection("appointments")

    val appointments = mutableStateListOf<Appointment>() // Use mutableStateListOf()

    try {
        val querySnapshot = collectionRef.get().await()  // Await Firestore query
        for (document in querySnapshot.documents) {
            val appointment = document.toObject(Appointment::class.java)
            appointment?.let { appointments.add(it) }
        }
        println("Appointments loaded: ${appointments.size}")
    } catch (exception: Exception) {
        println("Error getting documents: $exception")
    }

    return appointments
}*/


/*
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
*/

