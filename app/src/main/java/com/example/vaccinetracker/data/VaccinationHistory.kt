package com.example.vaccinetracker.data

data class VaccinationHistory(
    val vaccinationHistoryUid: String,
    val userUid: String,
    val vaccineUid: String,
    val dateAdministered: String,
    val doseNumber: Int
)
