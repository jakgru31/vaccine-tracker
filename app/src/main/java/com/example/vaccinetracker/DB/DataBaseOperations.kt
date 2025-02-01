import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.example.vaccinetracker.collections.Appointment
import com.example.vaccinetracker.collections.User
import com.example.vaccinetracker.collections.VaccinationRecord
import com.example.vaccinetracker.collections.Vaccine
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

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

            val records = db.collection("vaccination_records")
                .whereEqualTo("userUid", userId)
                .whereEqualTo("vaccineUd", vaccineId)
                .get()
                .await()

            // If there's already a record with this vaccineUid for the user, return false
            if (!appointments.isEmpty || !records.isEmpty) {
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



fun addVaccinesToFirestore() {

    val vaccinesList = listOf(
        Vaccine(vaccineId = "1", name = "COVID-19 Vaccine", manufacturer = "Pfizer", type = "mRNA", dosesRequired = 2, recommendedInterval = 21, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site")),
        Vaccine(vaccineId = "2", name = "Influenza Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness at injection site", "Mild fever", "Headache")),
        Vaccine(vaccineId = "3", name = "Hepatitis B Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 3, recommendedInterval = 30, commonSideEffects = listOf("Soreness at injection site", "Fatigue", "Headache")),
        Vaccine(vaccineId = "4", name = "Measles, Mumps, Rubella Vaccine (MMR)", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Fever", "Rash", "Soreness")),
        Vaccine(vaccineId = "5", name = "Polio Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 4, recommendedInterval = 60, commonSideEffects = listOf("Pain at injection site", "Fever")),
        Vaccine(vaccineId = "6", name = "Human Papillomavirus (HPV) Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Soreness at injection site", "Fever")),
        Vaccine(vaccineId = "7", name = "Diphtheria, Tetanus, and Pertussis Vaccine (DTaP)", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 5, recommendedInterval = 60, commonSideEffects = listOf("Soreness", "Swelling", "Fever")),
        Vaccine(vaccineId = "8", name = "Hepatitis A Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 2, recommendedInterval = 180, commonSideEffects = listOf("Soreness at injection site", "Headache", "Fatigue")),
        Vaccine(vaccineId = "9", name = "Rotavirus Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 3, recommendedInterval = 60, commonSideEffects = listOf("Fever", "Diarrhea", "Vomiting")),
        Vaccine(vaccineId = "10", name = "Varicella (Chickenpox) Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Rash", "Fever", "Tiredness")),
        Vaccine(vaccineId = "11", name = "Pneumococcal Vaccine", manufacturer = "Pfizer", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Redness at injection site", "Fever")),
        Vaccine(vaccineId = "12", name = "Meningococcal Vaccine", manufacturer = "Sanofi Pasteur", type = "Conjugate", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness at injection site", "Fever")),
        Vaccine(vaccineId = "13", name = "Shingles Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Pain at injection site", "Headache", "Fever")),
        Vaccine(vaccineId = "14", name = "Yellow Fever Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness at injection site", "Fever")),
        Vaccine(vaccineId = "15", name = "Typhoid Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness", "Fatigue", "Headache")),
        Vaccine(vaccineId = "16", name = "Bacillus Calmette-Guérin (BCG) Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Pain at injection site", "Fever")),
        Vaccine(vaccineId = "17", name = "Dengue Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Pain at injection site", "Headache", "Fatigue")),
        Vaccine(vaccineId = "18", name = "Cholera Vaccine", manufacturer = "Euvichol", type = "Inactivated", dosesRequired = 2, recommendedInterval = 30, commonSideEffects = listOf("Diarrhea", "Fever")),
        Vaccine(vaccineId = "19", name = "Zoster Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness", "Redness")),
        Vaccine(vaccineId = "20", name = "Smallpox Vaccine", manufacturer = "Bavarian Nordic", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Rash")),
        Vaccine(vaccineId = "21", name = "COVID-19 Vaccine", manufacturer = "Moderna", type = "mRNA", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site")),
        Vaccine(vaccineId = "22", name = "Hepatitis C Vaccine", manufacturer = "Gilead Sciences", type = "Inactivated", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fatigue", "Soreness")),
        Vaccine(vaccineId = "23", name = "Ebola Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Headache")),
        Vaccine(vaccineId = "24", name = "HIV Vaccine", manufacturer = "Various", type = "Inactivated", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Pain", "Swelling")),
        Vaccine(vaccineId = "25", name = "Rabies Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 5, recommendedInterval = 0, commonSideEffects = listOf("Pain at injection site", "Headache", "Fever")),
        Vaccine(vaccineId = "26", name = "Cervical Cancer Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Soreness at injection site", "Headache", "Dizziness")),
        Vaccine(vaccineId = "27", name = "Mumps Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness", "Fever")),
        Vaccine(vaccineId = "28", name = "Rubella Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Rash", "Fever", "Soreness")),
        Vaccine(vaccineId = "29", name = "Typhus Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness", "Headache")),
        Vaccine(vaccineId = "30", name = "Polio Vaccine (Oral)", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 4, recommendedInterval = 60, commonSideEffects = listOf("Mild fever", "Fatigue"))
    )

    val db = FirebaseFirestore.getInstance()
    val vaccinesCollection = db.collection("vaccine")

    vaccinesList.forEach { vaccine ->
        vaccinesCollection.document(vaccine.vaccineId).set(vaccine)
            .addOnSuccessListener {
                println("Successfully added vaccine: ${vaccine.name}")
            }
            .addOnFailureListener { e ->
                println("Error adding vaccine: ${e.message}")
            }
    }
}

/*fun generateQRCodeBitmap(data: String): Bitmap? {
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
}*/

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

