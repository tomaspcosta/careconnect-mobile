package com.example.careconnect.ui.screens.family

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.R
import com.example.careconnect.ui.components.bottomnav.FamilyBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FamilyOldAdultScreen(
    navController: NavHostController,
    patientUid: String?
) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf("patients") }
    var loading by remember { mutableStateOf(true) }
    var patient by remember { mutableStateOf<Map<String, Any>?>(null) }
    var tasks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var medications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var caregivers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showMedicationDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showAddCaregiverDialog by remember { mutableStateOf(false) }
    var unlinkCaregiverId by remember { mutableStateOf<String?>(null) }
    var unlinkCaregiverRole by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Daily, 1 = Monthly
    var dailyStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var monthlyStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    val scope = rememberCoroutineScope()

    // --- Extract caregivers fetch logic as a suspend lambda, not a composable ---
    suspend fun fetchCaregiversList(patientUid: String, setCaregivers: (List<Map<String, Any>>) -> Unit) {
        if (patientUid.isBlank()) return
        val caregiversList = mutableListOf<Map<String, Any>>()
        // Caregivers
        val cgLinks = db.collection("caregiver_patients").whereEqualTo("patient_id", patientUid).get().await()
        val caregiverIds = cgLinks.documents.mapNotNull { it.getString("caregiver_id") }.toSet()
        for (cid in caregiverIds) {
            val cgDoc = db.collection("users").document(cid).get().await()
            val cgData = cgDoc.data
            if (cgData != null) {
                val cgMap: MutableMap<String, Any> = mutableMapOf()
                for ((k, v) in cgData) {
                    cgMap[k] = v as Any
                }
                cgMap["uid"] = cid
                caregiversList.add(cgMap.toMap())
            }
        }
        // Family
        val famLinks = db.collection("family_patients").whereEqualTo("patient_id", patientUid).get().await()
        val familyIds = famLinks.documents.mapNotNull { it.getString("family_id") }.toSet()
        for (fid in familyIds) {
            val famDoc = db.collection("users").document(fid).get().await()
            val famData = famDoc.data
            if (famData != null) {
                val famMap: MutableMap<String, Any> = mutableMapOf()
                for ((k, v) in famData) {
                    famMap[k] = v as Any
                }
                famMap["uid"] = fid
                famMap["role"] = "family"
                caregiversList.add(famMap.toMap())
            }
        }
        setCaregivers(caregiversList)
    }

    fun fetchAll() {
        scope.launch {
            loading = true
            try {
                if (patientUid.isNullOrBlank()) {
                    loading = false
                    return@launch
                }
                // Patient
                val doc = db.collection("users").document(patientUid).get().await()
                val data = doc.data
                if (data != null) {
                    val map = data.toMutableMap()
                    map["uid"] = doc.id
                    patient = map.toMap()
                }
                // Tasks
                val snapshot = db.collection("tasks").whereEqualTo("patient_id", patientUid).get().await()
                tasks = snapshot.documents.mapNotNull { doc ->
                    val raw = doc.data ?: return@mapNotNull null
                    val map: MutableMap<String, Any> = mutableMapOf()
                    for ((k, v) in raw) {
                        map[k] = v as Any
                    }
                    map["id"] = doc.id
                    map.toMap()
                }
                // Medications
                val medSnapshot = db.collection("medications").whereEqualTo("patient_id", patientUid).get().await()
                medications = medSnapshot.documents.mapNotNull { doc ->
                    val raw = doc.data ?: return@mapNotNull null
                    val map: MutableMap<String, Any> = mutableMapOf()
                    for ((k, v) in raw) {
                        map[k] = v as Any
                    }
                    map["id"] = doc.id
                    map.toMap()
                }
                // Caregivers & Family
                val caregiversList = mutableListOf<Map<String, Any>>()
                // Caregivers
                val cgLinks = db.collection("caregiver_patients").whereEqualTo("patient_id", patientUid).get().await()
                val caregiverIds = cgLinks.documents.mapNotNull { it.getString("caregiver_id") }.toSet()
                for (cid in caregiverIds) {
                    val cgDoc = db.collection("users").document(cid).get().await()
                    val cgData = cgDoc.data
                    if (cgData != null) {
                        val cgMap: MutableMap<String, Any> = mutableMapOf()
                        for ((k, v) in cgData) {
                            cgMap[k] = v as Any
                        }
                        cgMap["uid"] = cid
                        caregiversList.add(cgMap.toMap())
                    }
                }
                // Family
                val famLinks = db.collection("family_patients").whereEqualTo("patient_id", patientUid).get().await()
                val familyIds = famLinks.documents.mapNotNull { it.getString("family_id") }.toSet()
                for (fid in familyIds) {
                    val famDoc = db.collection("users").document(fid).get().await()
                    val famData = famDoc.data
                    if (famData != null) {
                        val famMap: MutableMap<String, Any> = mutableMapOf()
                        for ((k, v) in famData) {
                            famMap[k] = v as Any
                        }
                        famMap["uid"] = fid
                        famMap["role"] = "family"
                        caregiversList.add(famMap.toMap())
                    }
                }
                caregivers = caregiversList
                // Alerts
                val generatedAlerts = mutableListOf<Map<String, Any>>()
                val patientName = patient?.get("name") ?: ""
                val taskSnapshot = db.collection("tasks").whereEqualTo("patient_id", patientUid).get().await()
                for (doc in taskSnapshot.documents) {
                    val data = doc.data ?: continue
                    if ((data["status"] ?: "") == "missed") {
                        generatedAlerts.add(mapOf(
                            "message" to "$patientName missed ${data["name"]} at ${data["time"] ?: ""}",
                            "date" to data["date"].toString()
                        ))
                    }
                }
                val checkinSnapshot = db.collection("checkin_logs").whereEqualTo("patient_id", patientUid).get().await()
                for (doc in checkinSnapshot.documents) {
                    val data = doc.data ?: continue
                    if ((data["status"] ?: "") == "missed") {
                        generatedAlerts.add(mapOf(
                            "message" to "$patientName missed ${data["timeOfDay"]} Check-In",
                            "date" to data["timestamp"].toString()
                        ))
                    }
                }
                val hydrationSnapshot = db.collection("hydration_logs").whereEqualTo("patient_id", patientUid).get().await()
                for (doc in hydrationSnapshot.documents) {
                    val data = doc.data ?: continue
                    if ((data["status"] ?: "") == "missed") {
                        generatedAlerts.add(mapOf(
                            "message" to "$patientName missed ${data["period"]} Hydration",
                            "date" to data["timestamp"].toString()
                        ))
                    }
                }
                val nutritionSnapshot = db.collection("nutrition_logs").whereEqualTo("patient_id", patientUid).get().await()
                for (doc in nutritionSnapshot.documents) {
                    val data = doc.data ?: continue
                    if ((data["status"] ?: "") == "missed") {
                        generatedAlerts.add(mapOf(
                            "message" to "$patientName missed ${(data["period"] ?: data["timeOfDay"])} Nutrition",
                            "date" to data["timestamp"].toString()
                        ))
                    }
                }
                alerts = generatedAlerts.sortedByDescending { it["date"].toString() }
            } catch (e: Exception) {
                loading = false
            }
            loading = false
        }
    }

    // --- Fetch daily and monthly stats ---
    suspend fun fetchStats(patientUid: String) {
        // --- Daily ---
        val today = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
        val daily = mutableMapOf<String, Any>()

        // Check-in
        val checkinLogs = db.collection("checkin_logs")
            .whereEqualTo("patient_id", patientUid)
            .get().await()
        val checkinToday = checkinLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == todayStr
        }
        val checkinMorning = checkinToday.any { (it.getString("timeOfDay") ?: "").lowercase() == "morning" && (it.getString("status") ?: "") == "confirmed" }
        val checkinEvening = checkinToday.any { (it.getString("timeOfDay") ?: "").lowercase() == "evening" && (it.getString("status") ?: "") == "confirmed" }
        daily["checkin"] = Pair(checkinMorning, checkinEvening)

        // Medication
        val medLogs = db.collection("tasks")
            .whereEqualTo("patient_id", patientUid)
            .get().await()
        val medsToday = medLogs.documents.filter {
            val ts = it.getTimestamp("date")?.toDate()
            ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == todayStr
        }
        val medsConfirmed = medsToday.count { (it.getString("status") ?: "") == "confirmed" }
        val medsTotal = medsToday.size
        daily["medication"] = Pair(medsConfirmed, medsTotal)

        // Hydration
        val hydrationLogs = db.collection("hydration_logs")
            .whereEqualTo("patient_id", patientUid)
            .get().await()
        val hydrationToday = hydrationLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == todayStr
        }
        val hydrationConfirmed = hydrationToday.count { (it.getString("status") ?: "") == "confirmed" }
        val hydrationTotal = hydrationToday.size
        daily["hydration"] = Pair(hydrationConfirmed, hydrationTotal)

        // Nutrition
        val nutritionLogs = db.collection("nutrition_logs")
            .whereEqualTo("patient_id", patientUid)
            .get().await()
        val nutritionToday = nutritionLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == todayStr
        }
        val nutritionConfirmed = nutritionToday.count { (it.getString("status") ?: "") == "confirmed" }
        val nutritionTotal = nutritionToday.size
        daily["nutrition"] = Pair(nutritionConfirmed, nutritionTotal)

        dailyStats = daily

        // --- Monthly ---
        val monthStats = mutableMapOf<String, Any>()
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val now = Date()

        // Check-in monthly
        val checkinMonth = checkinLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && ts >= monthStart && ts <= now
        }
        val checkinMorningMonth = checkinMonth.count { (it.getString("timeOfDay") ?: "").lowercase() == "morning" && (it.getString("status") ?: "") == "confirmed" }
        val checkinEveningMonth = checkinMonth.count { (it.getString("timeOfDay") ?: "").lowercase() == "evening" && (it.getString("status") ?: "") == "confirmed" }
        val checkinMorningTotal = checkinMonth.count { (it.getString("timeOfDay") ?: "").lowercase() == "morning" }
        val checkinEveningTotal = checkinMonth.count { (it.getString("timeOfDay") ?: "").lowercase() == "evening" }
        monthStats["checkin"] = Pair(Pair(checkinMorningMonth, checkinMorningTotal), Pair(checkinEveningMonth, checkinEveningTotal))

        // Medication monthly
        val medsMonth = medLogs.documents.filter {
            val ts = it.getTimestamp("date")?.toDate()
            ts != null && ts >= monthStart && ts <= now
        }
        val medsConfirmedMonth = medsMonth.count { (it.getString("status") ?: "") == "confirmed" }
        val medsTotalMonth = medsMonth.size
        monthStats["medication"] = Pair(medsConfirmedMonth, medsTotalMonth)

        // Hydration monthly
        val hydrationMonth = hydrationLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && ts >= monthStart && ts <= now
        }
        val hydrationConfirmedMonth = hydrationMonth.count { (it.getString("status") ?: "") == "confirmed" }
        val hydrationTotalMonth = hydrationMonth.size
        monthStats["hydration"] = Pair(hydrationConfirmedMonth, hydrationTotalMonth)

        // Nutrition monthly
        val nutritionMonth = nutritionLogs.documents.filter {
            val ts = it.getTimestamp("timestamp")?.toDate()
            ts != null && ts >= monthStart && ts <= now
        }
        val nutritionConfirmedMonth = nutritionMonth.count { (it.getString("status") ?: "") == "confirmed" }
        val nutritionTotalMonth = nutritionMonth.size
        monthStats["nutrition"] = Pair(nutritionConfirmedMonth, nutritionTotalMonth)

        monthlyStats = monthStats
    }

    // Fetch stats when patientUid changes
    LaunchedEffect(patientUid) {
        if (!patientUid.isNullOrBlank()) {
            scope.launch {
                fetchStats(patientUid)
            }
        }
    }

    // --- Existing fetchAll, LaunchedEffect, Scaffold ---
    LaunchedEffect(patientUid) {
        if (patientUid.isNullOrBlank()) {
            loading = false
        } else {
            fetchAll()
        }
    }

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
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (patientUid.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No patient selected.", color = Color.Gray)
            }
        } else if (patient == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Patient not found.", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Patient Info Card ---
                val imageUrl = patient?.get("profileImage") as? String
                val patientName = patient?.get("name") as? String ?: "Unnamed"
                val patientPhone = patient?.get("phone") as? String ?: "-"
                val patientEmail = patient?.get("email") as? String ?: "-"
                val patientAddress = patient?.get("address") as? String ?: "-"
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!imageUrl.isNullOrBlank()) {
                            Image(
                                painter = coil.compose.rememberAsyncImagePainter(imageUrl),
                                contentDescription = patientName,
                                modifier = Modifier.size(110.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_profile),
                                contentDescription = "Profile Image",
                                modifier = Modifier.size(110.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(patientName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(patientPhone, fontSize = 15.sp, color = Color(0xFF5A5A5A))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(patientEmail, fontSize = 15.sp, color = Color(0xFF5A5A5A))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(patientAddress, fontSize = 15.sp, color = Color(0xFF5A5A5A), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                // --- End Patient Info Card ---

                // --- Tabs for Daily/Monthly ---
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Daily", modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Monthly", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // --- Daily Stats ---
                    SummaryGridStats(dailyStats, isMonthly = false)
                } else {
                    // --- Monthly Stats ---
                    SummaryGridStats(monthlyStats, isMonthly = true)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showMedicationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add Medication",
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { showTaskDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add Task",
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // --- Add "Add Caregiver/Family" Button ---
                Button(
                    onClick = { showAddCaregiverDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Caregiver or Family Member", color = Color.White, fontSize = 15.sp)
                }

                // --- Caregivers & Family List with Unlink Button ---
                Text("Caregivers & Family", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF284545), modifier = Modifier.align(Alignment.Start))
                if (caregivers.isEmpty()) {
                    Text("No caregivers or family members associated.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(caregivers.size) { idx ->
                            val cgMap = caregivers[idx]
                            val cgImg = cgMap["profileImage"] as? String
                            val cgName = cgMap["name"] as? String ?: "-"
                            val cgPhone = cgMap["phone"] as? String ?: "-"
                            val cgRole = (cgMap["role"] as? String)?.lowercase() ?: "caregiver"
                            val roleLabel = when (cgRole) {
                                "family", "family_member" -> "Family Member"
                                else -> "Caregiver"
                            }
                            val cgUid = cgMap["uid"] as? String
                            val cgEmail = cgMap["email"] as? String ?: "-"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(270.dp) // Increased height for button visibility
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    val imageHeight = 250.dp * 0.4f
                                    if (cgImg != null && cgImg.startsWith("http")) {
                                        Image(
                                            painter = coil.compose.rememberAsyncImagePainter(cgImg),
                                            contentDescription = cgName,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(imageHeight)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(imageHeight)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                .background(Color(0xFFE0E0E0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF284545))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Order: profile picture -> role name -> email -> phone number
                                    Text(roleLabel, fontSize = 13.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(cgName, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Black)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(cgEmail, fontSize = 13.sp, color = Color(0xFF5A5A5A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(cgPhone, fontSize = 13.sp, color = Color(0xFF5A5A5A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // --- Unlink Button ---
                                    if (cgUid != null) {
                                        Button(
                                            onClick = { unlinkCaregiverId = cgUid; unlinkCaregiverRole = cgRole },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.LinkOff, contentDescription = null, tint = Color.White)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Unlink", color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Alerts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF284545), modifier = Modifier.align(Alignment.Start))
                if (alerts.isEmpty()) {
                    Text("No alerts.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        alerts.take(5).forEach { alert ->
                            val alertMap = alert
                            Text(alertMap["message"] as? String ?: "-", fontSize = 13.sp, color = Color.Red, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // --- Unlink Caregiver/Family Dialog ---
        if (unlinkCaregiverId != null && patientUid != null && unlinkCaregiverRole != null) {
            AlertDialog(
                onDismissRequest = { unlinkCaregiverId = null; unlinkCaregiverRole = null },
                title = { Text("Unlink ${if (unlinkCaregiverRole == "family") "Family Member" else "Caregiver"}", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to unlink this ${if (unlinkCaregiverRole == "family") "family member" else "caregiver"}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val db = Firebase.firestore
                            if (unlinkCaregiverRole == "family") {
                                db.collection("family_patients")
                                    .whereEqualTo("family_id", unlinkCaregiverId)
                                    .whereEqualTo("patient_id", patientUid)
                                    .get()
                                    .addOnSuccessListener { docs ->
                                        for (doc in docs) {
                                            db.collection("family_patients").document(doc.id).delete()
                                        }
                                    }
                            } else {
                                db.collection("caregiver_patients")
                                    .whereEqualTo("caregiver_id", unlinkCaregiverId)
                                    .whereEqualTo("patient_id", patientUid)
                                    .get()
                                    .addOnSuccessListener { docs ->
                                        for (doc in docs) {
                                            db.collection("caregiver_patients").document(doc.id).delete()
                                        }
                                    }
                            }
                            unlinkCaregiverId = null
                            unlinkCaregiverRole = null
                            fetchAll()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Unlink", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { unlinkCaregiverId = null; unlinkCaregiverRole = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- Add Caregiver/Family Dialog ---
        if (showAddCaregiverDialog && patientUid != null) {
            AddCaregiverOrFamilyDialog(
                patientUid = patientUid,
                onDismiss = { showAddCaregiverDialog = false },
                onCaregiverListChanged = {
                    // Only update caregivers list, not the whole page
                    scope.launch {
                        fetchCaregiversList(patientUid) { newList -> caregivers = newList }
                    }
                }
            )
        }

        // ...existing dialogs for medication/task...
        if (showMedicationDialog) {
            MedicationDialogFirestore(
                context = context,
                onDismiss = { showMedicationDialog = false },
                patientUid = patientUid,
                onAdded = { showMedicationDialog = false; fetchAll() }
            )
        }
        if (showTaskDialog) {
            TaskDialogFirestore(
                context = context,
                onDismiss = { showTaskDialog = false },
                patientUid = patientUid,
                onAdded = { showTaskDialog = false; fetchAll() }
            )
        }
    }
}

@Composable
fun SummaryGridStats(stats: Map<String, Any>, isMonthly: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Check-in
            if (!isMonthly) {
                val checkin = stats["checkin"] as? Pair<*, *> ?: Pair(false, false)
                val morning = checkin.first as? Boolean ?: false
                val evening = checkin.second as? Boolean ?: false
                CheckInStatusCard(
                    title = "Check-In",
                    morningConfirmed = morning,
                    eveningConfirmed = evening,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val checkin = stats["checkin"] as? Pair<*, *> ?: Pair(Pair(0,0), Pair(0,0))
                val (morningPair, eveningPair) = checkin
                val (morningConfirmed, morningTotal) = morningPair as? Pair<Int, Int> ?: Pair(0,0)
                val (eveningConfirmed, eveningTotal) = eveningPair as? Pair<Int, Int> ?: Pair(0,0)
                CheckInStatusCard(
                    title = "Check-In",
                    morningConfirmedCount = morningConfirmed,
                    morningTotal = morningTotal,
                    eveningConfirmedCount = eveningConfirmed,
                    eveningTotal = eveningTotal,
                    isMonthly = true,
                    modifier = Modifier.weight(1f)
                )
            }
            // Medication (improved display)
            val med = stats["medication"] as? Pair<Int, Int> ?: Pair(0,0)
            val medConfirmed = med.first
            val medTotal = med.second
            val medText = when {
                medTotal == 0 -> "No medication scheduled"
                medConfirmed == medTotal -> "All confirmed"
                medConfirmed == 0 -> "None confirmed"
                else -> "$medConfirmed confirmed\nof $medTotal"
            }
            SummaryCard("Medication", medText, Icons.Default.Medication, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hydration
            val hydration = stats["hydration"] as? Pair<Int, Int> ?: Pair(0,0)
            SummaryCard("Hydration", "${hydration.first} / ${hydration.second} confirmed", Icons.Default.InvertColors, modifier = Modifier.weight(1f))
            // Nutrition
            val nutrition = stats["nutrition"] as? Pair<Int, Int> ?: Pair(0,0)
            SummaryCard("Nutrition", "${nutrition.first} / ${nutrition.second} confirmed", Icons.Default.Restaurant, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CheckInStatusCard(
    title: String,
    morningConfirmed: Boolean = false,
    eveningConfirmed: Boolean = false,
    morningConfirmedCount: Int = 0,
    morningTotal: Int = 0,
    eveningConfirmedCount: Int = 0,
    eveningTotal: Int = 0,
    isMonthly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            if (!isMonthly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Morning", fontSize = 12.sp, color = Color.DarkGray)
                        Icon(
                            imageVector = if (morningConfirmed) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (morningConfirmed) "Confirmed" else "Missed",
                            tint = if (morningConfirmed) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Evening", fontSize = 12.sp, color = Color.DarkGray)
                        Icon(
                            imageVector = if (eveningConfirmed) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (eveningConfirmed) "Confirmed" else "Missed",
                            tint = if (eveningConfirmed) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Morning", fontSize = 12.sp, color = Color.DarkGray)
                        Text(
                            "$morningConfirmedCount/$morningTotal",
                            fontWeight = FontWeight.Bold,
                            color = if (morningTotal > 0 && morningConfirmedCount == morningTotal) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Evening", fontSize = 12.sp, color = Color.DarkGray)
                        Text(
                            "$eveningConfirmedCount/$eveningTotal",
                            fontWeight = FontWeight.Bold,
                            color = if (eveningTotal > 0 && eveningConfirmedCount == eveningTotal) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, info: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Text(
                text = info,
                fontSize = 12.sp,
                color = Color.DarkGray,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MedicationDialogFirestore(context: Context, onDismiss: () -> Unit, patientUid: String?, onAdded: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var firstHour by remember { mutableStateOf("") }
    var intervalHours by remember { mutableStateOf("") }
    var timesPerDay by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val db = Firebase.firestore
    val calendar = Calendar.getInstance()
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateSelected("${"%02d".format(d)}/${"%02d".format(m + 1)}/$y") },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onTimeSelected("${"%02d".format(h)}:${"%02d".format(m)}") },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun parseDate(date: String): Date? {
        return try {
            val parts = date.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (error.isNotEmpty()) {
                    Text(error, color = Color.Red, fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Medication Name *") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    placeholder = { Text("Dosage *") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description") },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        placeholder = { Text("Start Date *") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker { startDate = it } }
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {},
                        placeholder = { Text("End Date *") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker { endDate = it } }
                    )
                }
                OutlinedTextField(
                    value = firstHour,
                    onValueChange = {},
                    placeholder = { Text("First Hour *") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker { firstHour = it } }
                )
                // Interval (hours) field in its own row
                OutlinedTextField(
                    value = intervalHours,
                    onValueChange = { intervalHours = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("Interval (hours) *") },
                    leadingIcon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Times per day field in its own row
                OutlinedTextField(
                    value = timesPerDay,
                    onValueChange = { timesPerDay = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("Times per day *") },
                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // --- Buttons in the same row, 50/50 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)), // light red
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", color = Color(0xFFD32F2F))
                    }
                    Button(
                        onClick = {
                            if (name.isBlank() || dosage.isBlank() || startDate.isBlank() || endDate.isBlank() || firstHour.isBlank() || intervalHours.isBlank() || timesPerDay.isBlank()) {
                                error = "All required fields must be filled."
                                return@Button
                            }
                            val startDateObj = parseDate(startDate)
                            val endDateObj = parseDate(endDate)
                            if (startDateObj == null || endDateObj == null) {
                                error = "Invalid start or end date."
                                return@Button
                            }
                            if (patientUid != null && !adding) {
                                adding = true
                                scope.launch {
                                    val med = hashMapOf(
                                        "name" to name,
                                        "dosage" to dosage,
                                        "description" to description,
                                        "start_date" to com.google.firebase.Timestamp(startDateObj),
                                        "end_date" to com.google.firebase.Timestamp(endDateObj),
                                        "first_hour" to firstHour,
                                        "interval_hours" to intervalHours.toIntOrNull(),
                                        "times_per_day" to timesPerDay.toIntOrNull(),
                                        "patient_id" to patientUid,
                                        "taken" to false
                                    )
                                    db.collection("medications").add(med).await()
                                    adding = false
                                    onDismiss()
                                    onAdded()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)), // changed color here
                        shape = RoundedCornerShape(8.dp),
                        enabled = !adding && name.isNotBlank() && dosage.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank() && firstHour.isNotBlank() && intervalHours.isNotBlank() && timesPerDay.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (adding) "Adding..." else "Add", color = Color.White)
                    }
                }
            }
        },
        // Remove confirmButton and dismissButton from AlertDialog, handled above
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun TaskDialogFirestore(context: Context, onDismiss: () -> Unit, patientUid: String?, onAdded: () -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val db = Firebase.firestore
    val calendar = Calendar.getInstance()
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateSelected("${"%02d".format(d)}/${"%02d".format(m + 1)}/$y") },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onTimeSelected("${"%02d".format(h)}:${"%02d".format(m)}") },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun parseDateTime(date: String, time: String): Date? {
        return try {
            val parts = date.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            val tparts = time.split(":")
            val hour = tparts[0].toInt()
            val minute = tparts[1].toInt()
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (error.isNotEmpty()) {
                    Text(error, color = Color.Red, fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    placeholder = { Text("Task Name *") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { taskTime = it } }
                ) {
                    OutlinedTextField(
                        value = taskTime,
                        onValueChange = {},
                        placeholder = { Text("Select Time *") },
                        trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker { taskDate = it } }
                ) {
                    OutlinedTextField(
                        value = taskDate,
                        onValueChange = {},
                        placeholder = { Text("Select Date *") },
                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                // --- Buttons in the same row, 50/50 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)), // light red
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", color = Color(0xFFD32F2F))
                    }
                    Button(
                        onClick = {
                            if (taskName.isBlank() || taskDate.isBlank() || taskTime.isBlank()) {
                                error = "All required fields must be filled."
                                return@Button
                            }
                            val dateObj = parseDateTime(taskDate, taskTime)
                            if (dateObj == null) {
                                error = "Invalid date or time."
                                return@Button
                            }
                            if (patientUid != null && !adding) {
                                adding = true
                                scope.launch {
                                    val task = hashMapOf(
                                        "name" to taskName,
                                        "description" to description,
                                        "date" to com.google.firebase.Timestamp(dateObj),
                                        "time" to taskTime,
                                        "patient_id" to patientUid,
                                        "status" to "pending"
                                    )
                                    db.collection("tasks").add(task).await()
                                    adding = false
                                    onDismiss()
                                    onAdded()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)), // changed color here
                        shape = RoundedCornerShape(8.dp),
                        enabled = !adding && taskName.isNotBlank() && taskDate.isNotBlank() && taskTime.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (adding) "Adding..." else "Add", color = Color.White)
                    }
                }
            }
        },
        // Remove confirmButton and dismissButton from AlertDialog, handled above
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AddCaregiverOrFamilyDialog(
    patientUid: String,
    onDismiss: () -> Unit,
    onCaregiverListChanged: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            // Do not refresh the whole page, do not call onCaregiverListChanged
        },
        title = { Text("Add Caregiver or Family", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error.isNotEmpty()) {
                    Text(error, color = Color.Red, fontSize = 14.sp)
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("User Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank()) {
                        error = "Email is required."
                        return@Button
                    }
                    adding = true
                    scope.launch {
                        try {
                            val userQuery = db.collection("users").whereEqualTo("email", email).get().await()
                            if (userQuery.isEmpty) {
                                error = "No user found with this email."
                                adding = false
                                return@launch
                            }
                            val userDoc = userQuery.documents.first()
                            val userId = userDoc.id
                            val userRole = userDoc.getString("role") ?: ""
                            if (userRole != "caregiver" && userRole != "family") {
                                error = "Only caregiver or family member emails are allowed."
                                adding = false
                                return@launch
                            }
                            if (userRole == "caregiver") {
                                // Check if already linked
                                val existing = db.collection("caregiver_patients")
                                    .whereEqualTo("caregiver_id", userId)
                                    .whereEqualTo("patient_id", patientUid)
                                    .get().await()
                                if (!existing.isEmpty) {
                                    error = "This caregiver is already associated."
                                    adding = false
                                    return@launch
                                }
                                db.collection("caregiver_patients").add(
                                    mapOf("caregiver_id" to userId, "patient_id" to patientUid)
                                ).await()
                            } else {
                                // Check if already linked
                                val existing = db.collection("family_patients")
                                    .whereEqualTo("family_id", userId)
                                    .whereEqualTo("patient_id", patientUid)
                                    .get().await()
                                if (!existing.isEmpty) {
                                    error = "This family member is already associated."
                                    adding = false
                                    return@launch
                                }
                                db.collection("family_patients").add(
                                    mapOf("family_id" to userId, "patient_id" to patientUid)
                                ).await()
                            }
                            adding = false
                            onDismiss()
                            onCaregiverListChanged()
                        } catch (e: Exception) {
                            error = "Failed to add: ${e.localizedMessage}"
                            adding = false
                        }
                    }
                },
                enabled = !adding,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(if (adding) "Adding..." else "Add", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                // Do not refresh the whole page, do not call onCaregiverListChanged
            }) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}