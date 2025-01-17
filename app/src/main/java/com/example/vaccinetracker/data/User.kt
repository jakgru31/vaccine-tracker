package com.example.vaccinetracker.data

import com.google.firebase.firestore.FirebaseFirestore

data class User(
    val id: String = "",  // Firebase Auth UID
    val email: String = "",
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val certificates: MutableList<Certificate> = mutableListOf() // List to store certificates
) {
    companion object {
        fun fetchUserData(userId: String, callback: (User?) -> Unit) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    callback(user) // Return the user object
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    callback(null) // null in case of failure
                }
        }
    }
}
