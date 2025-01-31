package com.example.vaccinetracker.collections

data class Appointment(
    val appointmentId: String, // Unique ID for the appointment
    val userId: String, // Reference to the user who made the appointment
    val vaccineId: String,
    val appointmentDate: String, // Date and time of the appointment
)
