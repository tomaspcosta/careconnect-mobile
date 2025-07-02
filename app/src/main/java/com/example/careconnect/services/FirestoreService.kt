package com.example.careconnect.services

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    // Create Older Adult
    fun createOlderAdult(
        name: String,
        dob: String,
        caregiverId: String,
        familyId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "name" to name,
            "dob" to dob,
            "caregiverId" to caregiverId,
            "familyId" to familyId,
            "active" to true,
            "createdAt" to Timestamp.now()
        )

        db.collection("olderAdults")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Create Reminder
    fun createReminder(
        olderAdultId: String,
        label: String,
        time: String,
        createdBy: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "olderAdultId" to olderAdultId,
            "label" to label,
            "time" to time,
            "createdBy" to createdBy,
            "createdAt" to Timestamp.now()
        )

        db.collection("reminders")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Confirm Reminder
    fun confirmReminder(
        reminderId: String,
        olderAdultId: String,
        confirmedBy: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "reminderId" to reminderId,
            "olderAdultId" to olderAdultId,
            "confirmedBy" to confirmedBy,
            "confirmedAt" to Timestamp.now()
        )

        db.collection("confirmations")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Add a Check-in
    fun addCheckIn(
        olderAdultId: String,
        timeOfDay: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(now)

        val data = hashMapOf(
            "olderAdultId" to olderAdultId,
            "timeOfDay" to timeOfDay,
            "checkedInAt" to Timestamp.now(),
            "date" to dateStr
        )

        db.collection("checkIns")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Get today's check-ins for an older adult
    fun getTodaysCheckIns(
        olderAdultId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("checkIns")
            .whereEqualTo("olderAdultId", olderAdultId)
            .whereEqualTo("date", dateStr)
            .get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.documents.mapNotNull { it.data }
                onResult(data)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Generic fetch helper
    private fun getDocumentsByField(
        collection: String,
        field: String,
        value: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(collection)
            .whereEqualTo(field, value)
            .get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.documents.mapNotNull { it.data }
                onResult(data)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getReminders(
        olderAdultId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getDocumentsByField("reminders", "olderAdultId", olderAdultId, onResult, onFailure)

    fun getOlderAdultsByCaregiver(
        caregiverId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getDocumentsByField("olderAdults", "caregiverId", caregiverId, onResult, onFailure)

    fun getOlderAdultsByFamily(
        familyId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getDocumentsByField("olderAdults", "familyId", familyId, onResult, onFailure)

    fun getCaregiversByOlderAdult(
        olderAdultId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getDocumentsByField("caregivers", "olderAdultId", olderAdultId, onResult, onFailure)

    fun getFamiliesByOlderAdult(
        olderAdultId: String,
        onResult: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getDocumentsByField("families", "olderAdultId", olderAdultId, onResult, onFailure)
}
