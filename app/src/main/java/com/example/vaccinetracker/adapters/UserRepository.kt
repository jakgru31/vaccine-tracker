package com.example.vaccinetracker.data

import com.example.vaccinetracker.collections.User
import com.google.firebase.firestore.FirebaseFirestore

//TODO LOads users data to console
class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun fetchUserData(userId: String, callback: (User?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    callback(user)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}