package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
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

class NutritionViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _mealStatus = MutableStateFlow<Map<String, Boolean>>(
        mapOf("Breakfast" to false, "Lunch" to false, "Dinner" to false)
    )
    val mealStatus = _mealStatus.asStateFlow()

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
                    fetchNutritionLogs()
                } else {
                    Log.e("NutritionViewModel", "User document not found")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("NutritionViewModel", "Error loading user ID", e)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchNutritionLogs() {
        if (userId == null) return

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.time

        db.collection("nutrition_logs")
            .whereEqualTo("patient_id", userId)
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startOfDay))
            .whereLessThanOrEqualTo("timestamp", com.google.firebase.Timestamp(endOfDay))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("NutritionViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val newStatus = mutableMapOf("Breakfast" to false, "Lunch" to false, "Dinner" to false)
                snapshot?.documents?.forEach { doc ->
                    val period = doc.getString("period")
                    if (period != null && doc.getString("status") == "confirmed") {
                        newStatus[period] = true
                    }
                }
                _mealStatus.value = newStatus
                _isLoading.value = false
            }
    }

    fun confirmMeal(period: String) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val log = hashMapOf(
                    "patient_id" to userId,
                    "period" to period,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "confirmed"
                )
                db.collection("nutrition_logs").add(log).await()
            } catch (e: Exception) {
                Log.e("NutritionViewModel", "Error confirming meal", e)
            }
        }
    }
}

@Composable
fun OldAdultNutritionScreen(
    navController: NavHostController,
    viewModel: NutritionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("nutrition") }
    val mealStatus by viewModel.mealStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val breakfastTaken = mealStatus["Breakfast"] == true
    val lunchTaken = mealStatus["Lunch"] == true
    val dinnerTaken = mealStatus["Dinner"] == true

    val mealsConfirmed = listOf(breakfastTaken, lunchTaken, dinnerTaken).count { it }
    val nutritionProgress = mealsConfirmed / 3f

    val progressMessage = when (mealsConfirmed) {
        0 -> "You haven't confirmed any meals yet."
        1 -> "Great start! Keep it up."
        2 -> "Almost there! Just one meal left."
        3 -> "Excellent! All meals confirmed."
        else -> ""
    }

    fun isWithinMealTime(period: String): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (period) {
            "Breakfast" -> hour in 7..9
            "Lunch" -> hour in 12..14
            "Dinner" -> hour in 19..20
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
            Text(text = "Nutrition", fontSize = 22.sp, color = Color(0xFF284545))
            Text(
                text = "Track your meals for today",
                fontSize = 16.sp, color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 100.dp))
            } else {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "Fork and Knife",
                    tint = Color(0xFF425E57),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = nutritionProgress,
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFF425E57),
                    trackColor = Color(0xFF9BC6B2)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$mealsConfirmed / 3 meals confirmed\n$progressMessage",
                    color = Color(0xFF425E57),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NutritionItem(
                        label = "Breakfast",
                        taken = breakfastTaken,
                        enabled = isWithinMealTime("Breakfast") && !breakfastTaken,
                        onMarkTaken = { viewModel.confirmMeal("Breakfast") }
                    )
                    NutritionItem(
                        label = "Lunch",
                        taken = lunchTaken,
                        enabled = isWithinMealTime("Lunch") && !lunchTaken,
                        onMarkTaken = { viewModel.confirmMeal("Lunch") }
                    )
                    NutritionItem(
                        label = "Dinner",
                        taken = dinnerTaken,
                        enabled = isWithinMealTime("Dinner") && !dinnerTaken,
                        onMarkTaken = { viewModel.confirmMeal("Dinner") }
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionItem(label: String, taken: Boolean, enabled: Boolean, onMarkTaken: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = if (taken) Color(0xFF425E57) else Color(0xFFF7F7F7),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp),
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
                    containerColor = Color(0xFF425E57),
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
