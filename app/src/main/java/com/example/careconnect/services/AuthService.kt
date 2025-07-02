package com.example.careconnect.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Register user
    fun registerUserWithDetails(
        email: String,
        password: String,
        role: String,
        name: String,
        dob: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "role" to role,
                    "name" to name,
                    "dob" to dob,
                    "phone" to phone,
                    "createdAt" to Timestamp.now(),
                    "image" to "assets/images/default-profile-picture.png"
                )

                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        if (role == "older_adult") {
                            val olderAdultData = hashMapOf(
                                "name" to name,
                                "dob" to dob,
                                "active" to true,
                                "createdAt" to Timestamp.now()
                            )

                            firestore.collection("olderAdults").document(uid).set(olderAdultData)
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { e -> onFailure(e) }
                        } else {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Get current user's role
    fun getUserRole(
        onResult: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null)
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                onResult(role)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Logout
    fun signOut() {
        auth.signOut()
    }
}
