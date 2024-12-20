data class VaccinationHistory(
    val historyId: String, // Unique ID for history record
    val userId: String, // Reference to the user
    val vaccineName: String,
    val dateAdministered: String, // Date the vaccine was administered
    val doseNumber: Int // 1st dose, 2nd dose, etc.
)
