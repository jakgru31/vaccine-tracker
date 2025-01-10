

import Vaccine

data class VaccinationHistory(
    val historyId: String,
    val userId: String,
    val vaccine: Vaccine,
    val dateAdministered: String,
    val doseNumber: Int
)
