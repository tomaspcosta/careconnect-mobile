package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar
import com.example.careconnect.ui.navigation.Routes
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmergencyViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var patientId: String? = null
    private var patientName: String? = "Someone"

    init {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser?.email != null) {
                try {
                    val userQuery = db.collection("users")
                        .whereEqualTo("email", currentUser.email)
                        .limit(1)
                        .get().await()

                    if (userQuery.isEmpty) {
                        Log.e("EmergencyViewModel", "Patient document not found")
                        return@launch
                    }
                    val userDoc = userQuery.documents.first()
                    patientId = userDoc.getString("uid")
                    patientName = userDoc.getString("name") ?: "Someone"
                } catch (e: Exception) {
                    Log.e("EmergencyViewModel", "Error fetching patient details", e)
                }
            }
        }
    }

    fun sendEmergencyAlert(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (patientId == null) {
            onError("Could not identify the current user.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val relationsSnapshot = db.collection("caregiver_patients")
                    .whereEqualTo("patient_id", patientId)
                    .get().await()

                val caregiverIds = relationsSnapshot.documents.mapNotNull { it.getString("caregiver_id") }

                if (caregiverIds.isEmpty()) {
                    onError("No caregivers or family members are linked to receive alerts.")
                    _isLoading.value = false
                    return@launch
                }

                val batch = db.batch()
                for (caregiverId in caregiverIds) {
                    val notificationRef = db.collection("notifications").document()
                    val notificationData = hashMapOf(
                        "patient_id" to patientId,
                        "patient_name" to patientName,
                        "caregiver_id" to caregiverId,
                        "message" to "Emergency alert from $patientName!",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "type" to "emergency",
                        "isRead" to false
                    )
                    batch.set(notificationRef, notificationData)
                }


                batch.commit().await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error sending emergency alert", e)
                onError("Failed to send alert. Please try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}


@Composable
fun OldAdultEmergencyScreen(
    navController: NavHostController,
    viewModel: EmergencyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("help") } // 'help' is a more fitting default
    var showConfirmDialog by remember { mutableStateOf(false) }
    var alertSent by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val buttonColor by animateColorAsState(
        targetValue = if (isPressed.value) Color(0xFFB20000) else Color.Red
    )

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            OldAdultBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when(item) {
                        "dashboard" -> navController.navigate(Routes.OLD_ADULT) { launchSingleTop = true }
                        "caregivers" -> navController.navigate(Routes.OLD_ADULT_CAREGIVERS) { launchSingleTop = true }
                        "tasks" -> navController.navigate(Routes.OLD_ADULT_TASKS) { launchSingleTop = true }
                        "help" -> navController.navigate(Routes.OLD_ADULT_HELP) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.OLD_ADULT_PROFILE) { launchSingleTop = true }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Emergency",
                fontSize = 22.sp,
                color = Color(0xFF284545)
            )
            Text(
                text = "Only use this in case of emergency",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(350.dp)
                    .border(
                        width = 6.dp,
                        color = Color.LightGray,
                        shape = CircleShape
                    )
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(color = buttonColor, shape = CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isLoading
                    ) {
                        showConfirmDialog = true
                        alertSent = false
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = "SOS",
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (alertSent) {
                    Text(
                        text = "Emergency alert sent!",
                        color = Color.Red,
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Emergency") },
            text = { Text("Are you sure you want to send an emergency alert?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    viewModel.sendEmergencyAlert(
                        onSuccess = {
                            alertSent = true
                            Toast.makeText(context, "Alert sent successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }) {
                    Text("Yes, send alert")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
