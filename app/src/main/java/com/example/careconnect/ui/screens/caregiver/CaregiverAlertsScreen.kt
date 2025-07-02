package com.example.careconnect.ui.screens.caregiver


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.R
import com.example.careconnect.ui.components.bottomnav.FamilyBottomBar
import com.example.careconnect.ui.components.bottomnav.CaregiverBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

data class AlertDisplay(val docId: String, val message: String, val time: String, val date: String)

@Composable
fun FamilyAlertsScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("alerts") }

    val alerts = listOf(
        Triple("Manuel Oliveira", "missed a medication", "15:00"),
        Triple("João Sousa", "missed hydration", "10:45"),
        Triple("Manuel Oliveira", "missed a medication", "10:00"),
        Triple("Manuel Oliveira", "missed nutrition", "09:30"),
        Triple("Manuel Oliveira", "missed a medication", "yesterday"),
        Triple("Manuel Oliveira", "missed a medication", "yesterday")
    )

    Scaffold(
        bottomBar = {
            FamilyBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when (item) {
                        "dashboard" -> navController.navigate(Routes.FAMILY) { launchSingleTop = true }
                        "patients" -> navController.navigate(Routes.FAMILY_PATIENTS) { launchSingleTop = true }
                        "alerts" -> navController.navigate(Routes.FAMILY_ALERTS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.FAMILY_PROFILE) { launchSingleTop = true }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alerts",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF284545),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recent alerts from\n your older adults",
                fontSize = 20.sp,
                color = Color(0xFF284545),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            alerts.forEachIndexed { index, (name, message, time) ->
                AlertCard(name = name, message = message, time = time)
                if (index != alerts.lastIndex) {
                    Spacer(modifier = Modifier.height(20.dp)) // Espaço entre cartões
                }
            }
        }
    }
}

@Composable
fun AlertCard(name: String, message: String, time: String) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp)
                )

                Column {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = time,
                fontSize = 14.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun CaregiverAlertsScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("alerts") }
    var alerts by remember { mutableStateOf<List<AlertDisplay>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(user) {
        if (user != null) {
            loading = true
            try {
                val links = db.collection("caregiver_patients")
                    .whereEqualTo("caregiver_id", user.uid)
                    .get().await()
                val patientIds = links.documents.mapNotNull { it.getString("patient_id") }
                val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
                val alertList = mutableListOf<AlertDisplay>()
                for (pid in patientIds) {
                    val patientDoc = db.collection("users").document(pid).get().await()
                    val patientName = patientDoc.getString("name") ?: "Unknown"
                    // Missed tasks
                    val tasks = db.collection("tasks")
                        .whereEqualTo("patient_id", pid)
                        .whereEqualTo("date", today)
                        .whereEqualTo("status", "missed")
                        .get().await()
                    for (task in tasks.documents) {
                        val name = task.getString("name") ?: "Task"
                        val time = task.getString("time") ?: ""
                        alertList.add(AlertDisplay(task.id, "$patientName missed $name at $time", time, today))
                    }
                    // Missed hydration
                    val hydration = db.collection("hydration_logs")
                        .whereEqualTo("patient_id", pid)
                        .whereEqualTo("status", "missed")
                        .get().await()
                    for (log in hydration.documents) {
                        val ts = log.getTimestamp("timestamp")?.toDate()
                        val logDate = ts?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(it) } ?: ""
                        if (logDate == today) {
                            val periodField = log.get("period")
                            val period = if (periodField is String) periodField else "Hydration"
                            val time = ts?.let { String.format("%02d:%02d", it.hours, it.minutes) } ?: ""
                            alertList.add(AlertDisplay(log.id, "$patientName missed $period Hydration at $time", time, logDate))
                        }
                    }
                    // Missed nutrition
                    val nutrition = db.collection("nutrition_logs")
                        .whereEqualTo("patient_id", pid)
                        .whereEqualTo("status", "missed")
                        .get().await()
                    for (log in nutrition.documents) {
                        val ts = log.getTimestamp("timestamp")?.toDate()
                        val logDate = ts?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(it) } ?: ""
                        if (logDate == today) {
                            val periodField = log.get("period")
                            val period = if (periodField is String) periodField else (log.getString("timeOfDay") ?: "Nutrition")
                            val time = ts?.let { String.format("%02d:%02d", it.hours, it.minutes) } ?: ""
                            alertList.add(AlertDisplay(log.id, "$patientName missed $period Nutrition at $time", time, logDate))
                        }
                    }
                    // Missed check-ins
                    val checkins = db.collection("checkin_logs")
                        .whereEqualTo("patient_id", pid)
                        .whereEqualTo("status", "missed")
                        .get().await()
                    for (log in checkins.documents) {
                        val ts = log.getTimestamp("timestamp")?.toDate()
                        val logDate = ts?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(it) } ?: ""
                        if (logDate == today) {
                            val period = log.getString("timeOfDay") ?: "Check-In"
                            val time = ts?.let { String.format("%02d:%02d", it.hours, it.minutes) } ?: ""
                            alertList.add(AlertDisplay(log.id, "$patientName missed $period Check-In at $time", time, logDate))
                        }
                    }
                }
                alerts = alertList.sortedByDescending { it.time }
            } catch (e: Exception) {
                alerts = emptyList()
            }
            loading = false
        }
    }

    Scaffold(
        bottomBar = {
            CaregiverBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when (item) {
                        "dashboard" -> navController.navigate(Routes.CAREGIVER) { launchSingleTop = true }
                        "patients" -> navController.navigate(Routes.CAREGIVER_PATIENTS) { launchSingleTop = true }
                        "alerts" -> navController.navigate(Routes.CAREGIVER_ALERTS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.CAREGIVER_PROFILE) { launchSingleTop = true }
                    }
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alerts",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF284545),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recent alerts from your patients",
                fontSize = 20.sp,
                color = Color(0xFF284545),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (alerts.isEmpty()) {
                    Text("No alerts.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        alerts.forEachIndexed { index, alert ->
                            AlertCardWithDeleteAndDate(
                                docId = alert.docId,
                                message = alert.message,
                                time = alert.time,
                                date = alert.date,
                                onDelete = { id ->
                                    val collections = listOf("tasks", "hydration_logs", "nutrition_logs", "checkin_logs")
                                    for (col in collections) {
                                        db.collection(col).document(id).delete()
                                    }
                                    alerts = alerts.filterNot { it.docId == id }
                                }
                            )
                            if (index != alerts.lastIndex) {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCardWithDeleteAndDate(docId: String, message: String, time: String, date: String, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_alert),
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row {
                    Text(time, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(date, color = Color.Gray, fontSize = 14.sp)
                }
            }
            IconButton(onClick = { onDelete(docId) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alert",
                    tint = Color.Gray
                )
            }
        }
    }
}
