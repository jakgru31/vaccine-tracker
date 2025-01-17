package com.example.vaccinetracker.adapters

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.vaccinetracker.data.Certificate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CertificateAdapter {
    private val certificates: SnapshotStateList<Certificate> = mutableStateListOf()
    private var listenerRegistration: ListenerRegistration? = null

    fun addCertificates(newCertificates: List<Certificate>) {
        certificates.clear()
        certificates.addAll(newCertificates)
    }

    fun getCertificates(): List<Certificate> {
        return certificates
    }

    fun clearCertificates() {
        certificates.clear()
        listenerRegistration?.remove()
    }

    fun fetchCertificatesFromFirebase(callback: (List<Certificate>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("certificates")
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedCertificates = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Certificate::class.java)
                }
                callback(fetchedCertificates)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}
