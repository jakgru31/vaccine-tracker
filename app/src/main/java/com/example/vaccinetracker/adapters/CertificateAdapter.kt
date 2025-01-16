package com.example.vaccinetracker.adapters

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.vaccinetracker.data.Certificate

class CertificateAdapter {
    private val certificates: SnapshotStateList<Certificate> = mutableStateListOf()

    fun addCertificates(newCertificates: List<Certificate>) {
        certificates.addAll(newCertificates)
    }

    fun getCertificates(): List<Certificate> {
        return certificates
    }

    fun clearCertificates() {
        certificates.clear()
    }
}