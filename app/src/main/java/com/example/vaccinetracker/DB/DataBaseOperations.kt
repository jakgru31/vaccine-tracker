import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.example.vaccinetracker.collections.Appointment
import com.example.vaccinetracker.collections.User
import com.example.vaccinetracker.collections.VaccinationRecord
import com.example.vaccinetracker.collections.Vaccine
import com.google.ai.client.generativeai.GenerativeModel
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.io.println

//TODO Works
//suspend fun addNewUserToDatabase(user: User): Boolean { // Return success/failure
//    val db = FirebaseFirestore.getInstance()
//    val auth = FirebaseAuth.getInstance()
//
//    try {
//        val userCredential = auth.createUserWithEmailAndPassword(user.email, user.password).await()
//        val firebaseUser: FirebaseUser = userCredential.user!!
//        val uid = firebaseUser.uid
//
//        user.id = uid
//        db.collection("users").document(uid).set(user).await()
//
//        println("User added successfully")
//        return true // Success
//
//    } catch (e: Exception) {
//        val errorMessage = when (e) {
//            is com.google.firebase.firestore.FirebaseFirestoreException -> {
//                when (e.code) {
//                    com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "Document already exists."
//                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ALREADY_EXISTS -> "User with this email already exists." // More specific
//                    else -> "Firestore error: ${e.message}"
//                }
//            }
//            is FirebaseAuthException -> { // Handle FirebaseAuth exceptions
//                when (e.errorCode) {
//                    "ERROR_INVALID_EMAIL" -> "Invalid email format."
//                    "ERROR_WEAK_PASSWORD" -> "Password too weak."
//                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
//                    else -> "Authentication error: ${e.message}"
//                }
//            }
//
//            else -> "Error adding user: ${e.message}"
//
//        }
//        println("Error adding user: $errorMessage")
//        return false // Failure
//    }
//}

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
        Vaccine(name = "COVID-19 Vaccine", manufacturer = "Pfizer", type = "mRNA", dosesRequired = 2, recommendedInterval = 21, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site")),
        Vaccine(name = "Influenza Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness at injection site", "Mild fever", "Headache")),
        Vaccine(name = "Hepatitis B Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 3, recommendedInterval = 30, commonSideEffects = listOf("Soreness at injection site", "Fatigue", "Headache")),
        Vaccine(name = "Measles, Mumps, Rubella Vaccine (MMR)", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Fever", "Rash", "Soreness")),
        Vaccine(name = "Chickenpox Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Rash", "Fever", "Tiredness")),
        Vaccine(name = "Polio Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 4, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever")),
        Vaccine(name = "Hepatitis A Vaccine", manufacturer = "GlaxoSmithKline", type = "Inactivated", dosesRequired = 2, recommendedInterval = 180, commonSideEffects = listOf("Soreness at injection site", "Fatigue")),
        Vaccine(name = "Diphtheria, Tetanus, Pertussis (DTP) Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 5, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever", "Irritability")),
        Vaccine(name = "Haemophilus Influenzae Type B Vaccine", manufacturer = "GlaxoSmithKline", type = "Inactivated", dosesRequired = 3, recommendedInterval = 60, commonSideEffects = listOf("Fever", "Soreness")),
        Vaccine(name = "Meningococcal Vaccine", manufacturer = "Pfizer", type = "Conjugate", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Headache")),
        Vaccine(name = "Shingles Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Fever", "Tiredness")),
        Vaccine(name = "Yellow Fever Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Headache", "Tiredness")),
        Vaccine(name = "Rabies Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 3, recommendedInterval = 21, commonSideEffects = listOf("Redness at injection site", "Fever", "Headache")),
        Vaccine(name = "Typhoid Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Headache", "Soreness at injection site", "Fever")),
        Vaccine(name = "Rotavirus Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 3, recommendedInterval = 28, commonSideEffects = listOf("Diarrhea", "Vomiting", "Fever")),
        Vaccine(name = "Human Papillomavirus (HPV) Vaccine", manufacturer = "Merck", type = "Recombinant", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Pain at injection site", "Fatigue")),
        Vaccine(name = "Pneumococcal Vaccine", manufacturer = "Pfizer", type = "Conjugate", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Soreness", "Fever")),
        Vaccine(name = "Cervical Cancer Vaccine", manufacturer = "GlaxoSmithKline", type = "Recombinant", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Fatigue", "Pain at injection site", "Headache")),
        Vaccine(name = "Bacillus Calmette-Guérin (BCG) Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Soreness", "Low-grade fever")),
        Vaccine(name = "Pneumococcal 23-Valent Vaccine", manufacturer = "Pfizer", type = "Polysaccharide", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Muscle aches")),
        Vaccine(name = "Dengue Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Nausea", "Soreness at injection site")),
        Vaccine(name = "Tick-borne Encephalitis Vaccine", manufacturer = "Pfizer", type = "Inactivated", dosesRequired = 3, recommendedInterval = 21, commonSideEffects = listOf("Fatigue", "Fever", "Headache")),
        Vaccine(name = "Anthrax Vaccine", manufacturer = "Emergent BioSolutions", type = "Inactivated", dosesRequired = 6, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever", "Fatigue")),
        Vaccine(name = "Smallpox Vaccine", manufacturer = "Bavarian Nordic", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Rash")),
        Vaccine(name = "Cholera Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 2, recommendedInterval = 60, commonSideEffects = listOf("Diarrhea", "Abdominal pain", "Fever")),
        Vaccine(name = "Ebola Vaccine", manufacturer = "Johnson & Johnson", type = "Viral vector", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Pain at injection site", "Fatigue")),
        Vaccine(name = "Zika Virus Vaccine", manufacturer = "Inovio Pharmaceuticals", type = "DNA", dosesRequired = 2, recommendedInterval = 180, commonSideEffects = listOf("Fever", "Headache", "Soreness at injection site")),
        Vaccine(name = "Malaria Vaccine", manufacturer = "GSK", type = "Viral vector", dosesRequired = 4, recommendedInterval = 28, commonSideEffects = listOf("Fever", "Fatigue", "Pain at injection site")),
        Vaccine(name = "West Nile Virus Vaccine", manufacturer = "Pfizer", type = "Inactivated", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Fatigue", "Soreness at injection site")),
        Vaccine(name = "Avian Influenza Vaccine", manufacturer = "Boehringer Ingelheim", type = "Inactivated", dosesRequired = 2, recommendedInterval = 30, commonSideEffects = listOf("Fever", "Soreness at injection site", "Headache")),
        Vaccine(name = "COVID-19 Vaccine (Johnson & Johnson)", manufacturer = "Johnson & Johnson", type = "Viral vector", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site"))
    )


    val db = FirebaseFirestore.getInstance()
    val vaccinesCollection = db.collection("vaccines")

    vaccinesList.forEach { vaccine ->
        vaccinesCollection.add(vaccine)
            .addOnSuccessListener { documentReference ->
                println("Successfully added vaccine: ${vaccine.name} with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFirestoreException) {
                    println(" Firestore Error (${e.code}): ${e.message}")
                } else {
                    println(" Unexpected Error: ${e.localizedMessage}")
                }
                e.printStackTrace()
            }

    }
}


suspend fun generateChatBotSuggestions(prompt: String): String {

    val vaccinesList = listOf(
        Vaccine(name = "COVID-19 Vaccine", manufacturer = "Pfizer", type = "mRNA", dosesRequired = 2, recommendedInterval = 21, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site")),
        Vaccine(name = "Influenza Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 365, commonSideEffects = listOf("Soreness at injection site", "Mild fever", "Headache")),
        Vaccine(name = "Hepatitis B Vaccine", manufacturer = "Merck", type = "Inactivated", dosesRequired = 3, recommendedInterval = 30, commonSideEffects = listOf("Soreness at injection site", "Fatigue", "Headache")),
        Vaccine(name = "Measles, Mumps, Rubella Vaccine (MMR)", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Fever", "Rash", "Soreness")),
        Vaccine(name = "Chickenpox Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 2, recommendedInterval = 28, commonSideEffects = listOf("Rash", "Fever", "Tiredness")),
        Vaccine(name = "Polio Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 4, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever")),
        Vaccine(name = "Hepatitis A Vaccine", manufacturer = "GlaxoSmithKline", type = "Inactivated", dosesRequired = 2, recommendedInterval = 180, commonSideEffects = listOf("Soreness at injection site", "Fatigue")),
        Vaccine(name = "Diphtheria, Tetanus, Pertussis (DTP) Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 5, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever", "Irritability")),
        Vaccine(name = "Haemophilus Influenzae Type B Vaccine", manufacturer = "GlaxoSmithKline", type = "Inactivated", dosesRequired = 3, recommendedInterval = 60, commonSideEffects = listOf("Fever", "Soreness")),
        Vaccine(name = "Meningococcal Vaccine", manufacturer = "Pfizer", type = "Conjugate", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Headache")),
        Vaccine(name = "Shingles Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Fever", "Tiredness")),
        Vaccine(name = "Yellow Fever Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Headache", "Tiredness")),
        Vaccine(name = "Rabies Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 3, recommendedInterval = 21, commonSideEffects = listOf("Redness at injection site", "Fever", "Headache")),
        Vaccine(name = "Typhoid Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Headache", "Soreness at injection site", "Fever")),
        Vaccine(name = "Rotavirus Vaccine", manufacturer = "Merck", type = "Live", dosesRequired = 3, recommendedInterval = 28, commonSideEffects = listOf("Diarrhea", "Vomiting", "Fever")),
        Vaccine(name = "Human Papillomavirus (HPV) Vaccine", manufacturer = "Merck", type = "Recombinant", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Pain at injection site", "Fatigue")),
        Vaccine(name = "Pneumococcal Vaccine", manufacturer = "Pfizer", type = "Conjugate", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Soreness", "Fever")),
        Vaccine(name = "Cervical Cancer Vaccine", manufacturer = "GlaxoSmithKline", type = "Recombinant", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Fatigue", "Pain at injection site", "Headache")),
        Vaccine(name = "Bacillus Calmette-Guérin (BCG) Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Redness at injection site", "Soreness", "Low-grade fever")),
        Vaccine(name = "Pneumococcal 23-Valent Vaccine", manufacturer = "Pfizer", type = "Polysaccharide", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Muscle aches")),
        Vaccine(name = "Dengue Vaccine", manufacturer = "Sanofi Pasteur", type = "Live", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Nausea", "Soreness at injection site")),
        Vaccine(name = "Tick-borne Encephalitis Vaccine", manufacturer = "Pfizer", type = "Inactivated", dosesRequired = 3, recommendedInterval = 21, commonSideEffects = listOf("Fatigue", "Fever", "Headache")),
        Vaccine(name = "Anthrax Vaccine", manufacturer = "Emergent BioSolutions", type = "Inactivated", dosesRequired = 6, recommendedInterval = 60, commonSideEffects = listOf("Soreness at injection site", "Fever", "Fatigue")),
        Vaccine(name = "Smallpox Vaccine", manufacturer = "Bavarian Nordic", type = "Live", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Soreness at injection site", "Fever", "Rash")),
        Vaccine(name = "Cholera Vaccine", manufacturer = "Sanofi Pasteur", type = "Inactivated", dosesRequired = 2, recommendedInterval = 60, commonSideEffects = listOf("Diarrhea", "Abdominal pain", "Fever")),
        Vaccine(name = "Ebola Vaccine", manufacturer = "Johnson & Johnson", type = "Viral vector", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fever", "Pain at injection site", "Fatigue")),
        Vaccine(name = "Zika Virus Vaccine", manufacturer = "Inovio Pharmaceuticals", type = "DNA", dosesRequired = 2, recommendedInterval = 180, commonSideEffects = listOf("Fever", "Headache", "Soreness at injection site")),
        Vaccine(name = "Malaria Vaccine", manufacturer = "GSK", type = "Viral vector", dosesRequired = 4, recommendedInterval = 28, commonSideEffects = listOf("Fever", "Fatigue", "Pain at injection site")),
        Vaccine(name = "West Nile Virus Vaccine", manufacturer = "Pfizer", type = "Inactivated", dosesRequired = 3, recommendedInterval = 180, commonSideEffects = listOf("Headache", "Fatigue", "Soreness at injection site")),
        Vaccine(name = "Avian Influenza Vaccine", manufacturer = "Boehringer Ingelheim", type = "Inactivated", dosesRequired = 2, recommendedInterval = 30, commonSideEffects = listOf("Fever", "Soreness at injection site", "Headache")),
        Vaccine(name = "COVID-19 Vaccine (Johnson & Johnson)", manufacturer = "Johnson & Johnson", type = "Viral vector", dosesRequired = 1, recommendedInterval = 0, commonSideEffects = listOf("Fatigue", "Headache", "Pain at injection site"))
    )

    var availableVaccines = ""
    vaccinesList.forEach { vaccine -> availableVaccines += "${vaccine.name}, "}

    val finalPrompt = "\"You are a AI Assistant that helps users with vaccine recommendations." +
            " Here is a list of available vaccines: $availableVaccines." +
            " The user's query will appear after the colon." +
            " Your task is to analyze their question and provide useful tips or recommendations" +
            " related to vaccines. You should only suggest vaccines from the provided list" +
            " and avoid mentioning any that are not included. Ensure your response " +
            " is clear, concise, and medically relevant.\n" +
            "\n" +
            "User query: $prompt\""
    val apiKey = "" // Replace with your actual API key
    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    return try {
        val response = generativeModel.generateContent(finalPrompt)

        // Function to clean up markdown-style formatting
        fun cleanResponse(text: String): String {
            return text
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold (**text**)
                .replace(Regex("\\* (.*?)"), "$1") // Remove bullet points (* text)
        }

// Apply cleanup before displaying
        val formattedResponse = cleanResponse(response.text.toString())

        formattedResponse // Ensure proper string conversion
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message}"
    }
}



suspend fun loadAppointmentsForOneUser(userId: String): List<Appointment> {
    val db = FirebaseFirestore.getInstance()
    return try {
        // Query the "appointments" collection with the specified userId
        val snapshot = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Map the documents to Appointment objects and sort by appointmentDate
        snapshot.documents.mapNotNull { it.toObject(Appointment::class.java) }
            .sortedBy { it.appointmentDate } // Sort appointments by date (ascending order)
    } catch (e: Exception) {
        e.printStackTrace()
        println("Error fetching appointments: ${e.message}")
        emptyList()
    }
}



