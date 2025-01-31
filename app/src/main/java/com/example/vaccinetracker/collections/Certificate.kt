package com.example.vaccinetracker.collections

data class Certificate(
    val certificateId: String,
    val userId: String,
    val vaccineName: String,
    val dateAdministered: String,
    val doseNumber: Int,
    val qrCodeData: String // QR code data link or unique identifier
)
