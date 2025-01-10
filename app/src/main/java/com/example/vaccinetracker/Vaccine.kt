data class Vaccine(
    val vaccineId: String,
    val name: String,
    val manufacturer: String,
    val type: String,
    val dosesRequired: Int,
    val recommendedInterval: Int,
    val commonSideEffects: List<String>
)