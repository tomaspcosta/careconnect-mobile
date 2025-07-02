package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit


data class CheckInModel(
    val id: String = "",
    val patientId: String = "",
    val timestamp: Date = Date(),
    val period: String = "",
    val status: String = ""
)


class CheckInViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _checkInStatus = MutableStateFlow<Map<String, Boolean>>(
        mapOf("Morning" to false, "Evening" to false)
    )
    val checkInStatus = _checkInStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime = _currentTime.asStateFlow()

    private var userId: String? = null

    init {
        viewModelScope.launch {
            loadUserIdAndFetchLogs()
            while (true) {
                delay(1000)
                _currentTime.value = System.currentTimeMillis()
            }
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
                    fetchCheckInLogs()
                } else {
                    Log.e("CheckInViewModel", "User document not found")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CheckInViewModel", "Error loading user ID", e)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchCheckInLogs() {
        if (userId == null) {
            _isLoading.value = false
            return
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfDay = calendar.time

        db.collection("checkin_logs")
            .whereEqualTo("patient_id", userId)
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startOfDay))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("CheckInViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                val newStatus = mutableMapOf("Morning" to false, "Evening" to false)
                snapshot?.documents?.forEach { doc ->
                    val period = doc.getString("period")
                    if (period != null) {
                        newStatus[period] = true
                    }
                }
                _checkInStatus.value = newStatus
                _isLoading.value = false
            }
    }

    fun addCheckIn(period: String) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val log = hashMapOf(
                    "patient_id" to userId,
                    "period" to period,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "confirmed"
                )
                db.collection("checkin_logs").add(log).await()
            } catch (e: Exception) {
                Log.e("CheckInViewModel", "Error adding check-in", e)
            }
        }
    }
}

private data class ThemeSettings(
    val greeting: String,
    val backgroundColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val iconTint: Color
)

@Composable
fun OldAdultCheckInScreen(
    navController: NavHostController,
    viewModel: CheckInViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val checkInStatus by viewModel.checkInStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()

    val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    val theme = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> ThemeSettings("Good Morning!", Color(0xFFEAF6F6), Color.Black, Icons.Default.WbSunny, Color(0xFFFFC107))
            in 12..17 -> ThemeSettings("Good Afternoon!", Color(0xFFFFF9C4), Color.Black, Icons.Default.WbSunny, Color(0xFFFBC02D))
            else -> ThemeSettings("Good Evening!", Color(0xFF2C3E50), Color.White, Icons.Default.Brightness2, Color(0xFFB0BEC5))
        }
    }

    var selectedItem by remember { mutableStateOf("dashboard") }

    val nextCheckInPeriod = remember(checkInStatus, currentHour) {
        val morningConfirmed = checkInStatus["Morning"] == true
        val eveningConfirmed = checkInStatus["Evening"] == true

        if (currentHour < 10 && !morningConfirmed) {
            "Morning"
        } else if (currentHour < 20 && !eveningConfirmed) {
            "Evening"
        } else if (!morningConfirmed) {
            "Morning"
        } else if (!eveningConfirmed) {
            "Evening"
        }
        else {
            null
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = theme.icon,
                    contentDescription = theme.greeting,
                    tint = theme.iconTint,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = theme.greeting,
                    fontSize = 28.sp,
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Please check in for your next scheduled time.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = theme.textColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.weight(1f))

                if (nextCheckInPeriod != null) {
                    CheckInCard(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        period = nextCheckInPeriod,
                        timeRange = if (nextCheckInPeriod == "Morning") "8 AM - 10 AM" else "6 PM - 8 PM",
                        isConfirmed = checkInStatus[nextCheckInPeriod] == true,
                        currentTime = currentTime,
                        onCheckIn = { viewModel.addCheckIn(nextCheckInPeriod) }
                    )
                } else {
                    Text(
                        text = "All check-ins for today are complete. See you tomorrow!",
                        fontSize = 18.sp,
                        color = theme.textColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
fun CheckInCard(
    modifier: Modifier = Modifier,
    period: String,
    timeRange: String,
    isConfirmed: Boolean,
    currentTime: Long,
    onCheckIn: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    val (unlockHour, lockHour) = when (period) {
        "Morning" -> 8 to 10
        "Evening" -> 18 to 20
        else -> 0 to 0
    }

    val isButtonEnabled = currentHour >= unlockHour && currentHour < lockHour && !isConfirmed

    fun getCountdown(): String {
        val now = Calendar.getInstance().apply { timeInMillis = currentTime }
        val unlockTime = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, unlockHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If current time is past today's unlock time, set for the next day
        if (now.after(unlockTime) && now.get(Calendar.HOUR_OF_DAY) >= lockHour) {
            unlockTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diff = unlockTime.timeInMillis - now.timeInMillis
        if (diff <= 0) return "Available Now"

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Card(
        modifier = modifier.height(240.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(period, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(timeRange, fontSize = 16.sp, color = Color.Gray)

            if (isConfirmed) {
                Icon(Icons.Filled.Check, "Confirmed", modifier = Modifier.size(52.dp), tint = Color(0xFF4CAF50))
                Text("Confirmed!", color = Color(0xFF4CAF50), fontSize = 18.sp)
            } else {
                Button(
                    onClick = onCheckIn,
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF284545),
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text("I'm OK", color = Color.White, fontSize = 18.sp)
                }

                if (!isButtonEnabled) {
                    Text(
                        text = "Unlocks in:\n${getCountdown()}",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
