package com.example.vaccinetracker.collections

import android.annotation.SuppressLint
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class Appointment(
    @PropertyName("appointmentId") val appointmentId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("vaccineId") val vaccineId: String = "",
    @PropertyName("appointmentDate") val appointmentDate: Timestamp = Timestamp.now() // Firestore Timestamp
) : Serializable {
    // Firestore requires a no-arg constructor
    constructor() : this("", "", "", Timestamp.now())

    // Convert Firestore Timestamp to LocalDateTime
    @SuppressLint("NewApi")
    fun getLocalDateTime(): LocalDateTime {
        return appointmentDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
