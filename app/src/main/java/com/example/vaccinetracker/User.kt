package com.example.vaccinetracker

import java.util.Date

data class User(
    val id: String = "",  // Firebase Auth UID
    val email: String = "",
    val name: String = "", // You can expand this based on your requirements
    val dateOfBirth: String = ""
)