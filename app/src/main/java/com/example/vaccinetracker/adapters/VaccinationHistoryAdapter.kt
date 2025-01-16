package com.example.vaccinetracker.adapters

import androidx.compose.runtime.mutableStateListOf
import com.example.vaccinetracker.data.VaccinationHistory
import androidx.compose.runtime.snapshots.SnapshotStateList

class VaccinationHistoryAdapter {
    private val vaccinationHistory: SnapshotStateList<VaccinationHistory> = mutableStateListOf()

    fun addVaccinationHistory(history: VaccinationHistory) {
        vaccinationHistory.add(history)
    }

    fun getVaccinationHistory(): List<VaccinationHistory> {
        return vaccinationHistory
    }

    fun clearVaccinationHistory() {
        vaccinationHistory.clear()
    }
}