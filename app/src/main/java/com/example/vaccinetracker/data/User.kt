package com.example.vaccinetracker.data

data class User(
    var id: String = "",  // Firebase Auth UID
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val vaccinationHistories: MutableList<VaccinationHistory> = mutableListOf() // List of Histories for
// this user where one vaccinationHistory containts data about one vaccine the user made
)
