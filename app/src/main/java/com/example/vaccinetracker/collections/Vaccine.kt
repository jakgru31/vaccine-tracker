package com.example.vaccinetracker.collections

data class Vaccine(
    val name: String = "",
    val manufacturer: String = "",
    val type: String = "",
    val dosesRequired: Int = 0,
    val recommendedInterval: Int = 0,
    val commonSideEffects: List<String> = emptyList(),
)