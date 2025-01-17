package com.example.vaccinetracker.adapters

import androidx.compose.runtime.mutableStateListOf
import com.example.vaccinetracker.data.VaccinationHistory
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.firestore.FirebaseFirestore

class VaccinationHistoryAdapter {
    private val vaccinationHistory: SnapshotStateList<VaccinationHistory> = mutableStateListOf()

    fun addVaccinationHistory(history: VaccinationHistory) {
        vaccinationHistory.add(history)
    }

    fun addVaccinationHistory(histories: List<VaccinationHistory>) {
        vaccinationHistory.addAll(histories)
    }

    fun getVaccinationHistory(): List<VaccinationHistory> {
        return vaccinationHistory
    }

    fun clearVaccinationHistory() {
        vaccinationHistory.clear()
    }

    fun fetchVaccinationHistoryFromFirebase(
        userId: String,
        callback: (List<VaccinationHistory>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("vaccination_history")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedHistory = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(VaccinationHistory::class.java)
                }
                callback(fetchedHistory)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

}
