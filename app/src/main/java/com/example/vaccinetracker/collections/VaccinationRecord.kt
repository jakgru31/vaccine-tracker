package com.example.vaccinetracker.collections

data class VaccinationRecord(
    val vaccinationRecordUid: String,
    val userUid: String,
    val vaccineUid: String,
    val dateAdministered: String,
    val doseNumber: Int
)
