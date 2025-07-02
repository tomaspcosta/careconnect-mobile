package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*


class HydrationViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _hydrationStatus = MutableStateFlow<Map<String, Boolean>>(
        mapOf("Morning" to false, "Afternoon" to false, "Evening" to false)
    )
    val hydrationStatus = _hydrationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var userId: String? = null

    init {
        viewModelScope.launch {
            loadUserIdAndFetchLogs()
        }
    }

    private suspend fun loadUserIdAndFetchLogs() {
        _isLoading.value = true
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            try {
                val userQuery = db.collection("users")
                    .whereEqualTo("email", currentUser.email)
                    .limit(1)
                    .get()
                    .await()
                if (!userQuery.isEmpty) {
                    userId = userQuery.documents.first().getString("uid")
                    fetchHydrationLogs()
                } else {
                    Log.e("HydrationViewModel", "User document not found")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("HydrationViewModel", "Error loading user ID", e)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchHydrationLogs() {
        if (userId == null) return

        // Get start and end of the current day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.time

        db.collection("hydration_logs")
            .whereEqualTo("patient_id", userId)
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startOfDay))
            .whereLessThanOrEqualTo("timestamp", com.google.firebase.Timestamp(endOfDay))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HydrationViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val newStatus = mutableMapOf("Morning" to false, "Afternoon" to false, "Evening" to false)
                snapshot?.documents?.forEach { doc ->
                    val period = doc.getString("period")
                    if (period != null && doc.getString("status") == "confirmed") {
                        newStatus[period] = true
                    }
                }
                _hydrationStatus.value = newStatus
                _isLoading.value = false
            }
    }

    fun confirmHydration(period: String) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val amounts = mapOf("Morning" to 750, "Afternoon" to 1000, "Evening" to 250)
                val log = hashMapOf(
                    "patient_id" to userId,
                    "period" to period,
                    "amount_ml" to amounts[period],
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "confirmed"
                )
                db.collection("hydration_logs").add(log).await()
            } catch (e: Exception) {
                Log.e("HydrationViewModel", "Error confirming hydration", e)
            }
        }
    }
}

@Composable
fun OldAdultHydrationScreen(
    navController: NavHostController,
    viewModel: HydrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("hydration") }
    val hydrationStatus by viewModel.hydrationStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val morningTaken = hydrationStatus["Morning"] == true
    val afternoonTaken = hydrationStatus["Afternoon"] == true
    val eveningTaken = hydrationStatus["Evening"] == true

    val takenCount = listOf(morningTaken, afternoonTaken, eveningTaken).count { it }
    val hydrationProgress = takenCount / 3f

    val progressMessage = when (takenCount) {
        0 -> "You haven't marked any water intake yet."
        1 -> "Good start! Keep hydrated."
        2 -> "Almost there! Just one more time."
        3 -> "Great! You are fully hydrated today."
        else -> ""
    }

    fun isWithinTimeRange(period: String): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (period) {
            "Morning" -> hour in 8..11
            "Afternoon" -> hour in 12..17
            "Evening" -> hour in 18..21
            else -> false
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            OldAdultBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    when (item) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Hydration", fontSize = 22.sp, color = Color(0xFF284545))
            Text(
                text = "Track how much water you've had today",
                fontSize = 16.sp, color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 100.dp))
            } else {
                Icon(
                    imageVector = Icons.Filled.InvertColors,
                    contentDescription = "Water Drop",
                    tint = Color(0xFF1E56DD),
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = hydrationProgress,
                    modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)),
                    color = Color(0xFF1E56DD),
                    trackColor = Color(0xFFD6E3FF)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "${takenCount} / 3 times marked\n$progressMessage",
                    color = Color(0xFF1E56DD),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HydrationItem(
                        label = "Morning",
                        taken = morningTaken,
                        enabled = isWithinTimeRange("Morning") && !morningTaken,
                        onMarkTaken = { viewModel.confirmHydration("Morning") }
                    )
                    HydrationItem(
                        label = "Afternoon",
                        taken = afternoonTaken,
                        enabled = isWithinTimeRange("Afternoon") && !afternoonTaken,
                        onMarkTaken = { viewModel.confirmHydration("Afternoon") }
                    )
                    HydrationItem(
                        label = "Evening",
                        taken = eveningTaken,
                        enabled = isWithinTimeRange("Evening") && !eveningTaken,
                        onMarkTaken = { viewModel.confirmHydration("Evening") }
                    )
                }
            }
        }
    }
}

@Composable
fun HydrationItem(label: String, taken: Boolean, enabled: Boolean, onMarkTaken: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = if (taken) Color(0xFF284545) else Color(0xFFF7F7F7),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = if (taken) Color.White else Color.Black, fontSize = 18.sp)
        if (taken) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Taken",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Button(
                onClick = onMarkTaken,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF284545),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Mark as taken", fontSize = 14.sp)
            }
        }
    }
}
