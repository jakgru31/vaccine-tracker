package com.example.vaccinetracker.collections

data class User(
    var id: String = "",  // Firebase Auth UID
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val vaccinationRecords: MutableList<String> = mutableListOf(),
    val appointments: MutableList<String> = mutableListOf()// List of Histories for
// this user where one vaccinationHistory containts data about one vaccine the user made
)
