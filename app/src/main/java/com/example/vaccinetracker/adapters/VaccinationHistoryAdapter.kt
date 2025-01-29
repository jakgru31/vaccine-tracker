package com.example.vaccinetracker.adapters

import androidx.compose.runtime.mutableStateListOf
import com.example.vaccinetracker.data.VaccinationRecord
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.firestore.FirebaseFirestore

class VaccinationHistoryAdapter {
    private val vaccinationHistory: SnapshotStateList<VaccinationRecord> = mutableStateListOf()

    fun addVaccinationRecord(record: VaccinationRecord) {
        vaccinationHistory.add(record)
    }

    fun addVaccinationRecord(histories: List<VaccinationRecord>) {
        vaccinationHistory.addAll(histories)
    }

    fun getVaccinationRecord(): List<VaccinationRecord> {
        return vaccinationHistory
    }

    fun clearVaccinationRecord() {
        vaccinationHistory.clear()
    }

    fun fetchVaccinationRecordFromFirebase(
        userId: String,
        callback: (List<VaccinationRecord>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("vaccination_records")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedRecord = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(VaccinationRecord::class.java)
                }
                callback(fetchedRecord)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

}
