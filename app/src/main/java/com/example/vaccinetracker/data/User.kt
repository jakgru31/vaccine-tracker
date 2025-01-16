package com.example.vaccinetracker.data

data class User(
    val id: String = "",  // Firebase Auth UID
    val email: String = "",
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val certificates: MutableList<Certificate> = mutableListOf() // List to store certificates
)
