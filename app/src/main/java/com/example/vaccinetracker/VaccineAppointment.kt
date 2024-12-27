data class VaccineAppointment(
    val appointmentId: String, // Unique ID for the appointment
    val userId: String, // Reference to the user who made the appointment
    val vaccineName: String,
    val appointmentDate: String, // Date and time of the appointment
    val status: String // Pending, Completed, etc.
)
