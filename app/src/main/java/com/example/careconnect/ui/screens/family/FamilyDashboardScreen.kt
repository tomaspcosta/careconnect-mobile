package com.example.careconnect.ui.screens.family

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.careconnect.ui.components.bottomnav.FamilyBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDashboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    var patients by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedPatient by remember { mutableStateOf<Map<String, Any>?>(null) }
    var dailyStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var monthlyStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var linkDialogOpen by remember { mutableStateOf(false) }
    var linkEmail by remember { mutableStateOf("") }
    var linking by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var bottomNavSelected by remember { mutableStateOf("dashboard") }
    var familyName by remember { mutableStateOf("") }
    // Fetch family member name
    LaunchedEffect(user) {
        if (user != null) {
            val doc = db.collection("users").document(user.uid).get().await()
            familyName = doc.getString("name") ?: "Family Member"
        }
    }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Fetch linked patients using join collection
    suspend fun fetchLinkedPatients() {
        if (user != null) {
            try {
                val familyId = user.uid
                val links = db.collection("family_patients")
                    .whereEqualTo("family_id", familyId)
                    .get().await()
                val patientIds = links.documents.mapNotNull { it.getString("patient_id") }.toSet()
                val patientsList = mutableListOf<Map<String, Any>>()
                for (pid in patientIds) {
                    val patientDoc = db.collection("users").document(pid).get().await()
                    val data = patientDoc.data
                    if (data != null) {
                        patientsList.add(data + mapOf("uid" to pid))
                    }
                }
                patients = patientsList
                // Only set selectedPatient if it's not already set or if the selected patient is not in the new list
                if (selectedPatient == null || patientsList.none { it["uid"] == selectedPatient?.get("uid") }) {
                    selectedPatient = patientsList.firstOrNull()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                patients = emptyList()
                selectedPatient = null
            }
        }
    }

    // Fetch daily and monthly stats for selected patient
    suspend fun fetchStatsForPatient(patient: Map<String, Any>?) {
        if (patient != null) {
            val db = Firebase.firestore
            val patientId = patient["uid"] as String
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val daily = mutableMapOf<String, Any>()
            val monthStats = mutableMapOf<String, Any>()
            val now = Date()
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // --- Daily ---
            // Check-in
            val checkinLogs = db.collection("checkin_logs").whereEqualTo("patient_id", patientId).get().await()
            val checkinToday = checkinLogs.documents.filter {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            val checkinMorning = checkinToday.any { (it.getString("timeOfDay") ?: "").lowercase() == "morning" && (it.getString("status") ?: "") == "confirmed" }
            val checkinEvening = checkinToday.any { (it.getString("timeOfDay") ?: "").lowercase() == "evening" && (it.getString("status") ?: "") == "confirmed" }
            daily["checkin"] = Pair(checkinMorning, checkinEvening)

            // Medication
            val medLogs = db.collection("tasks").whereEqualTo("patient_id", patientId).get().await()
            val medsToday = medLogs.documents.filter {
                val ts = it.getTimestamp("date")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            val medsConfirmed = medsToday.count { (it.getString("status") ?: "") == "confirmed" }
            val medsTotal = medsToday.size
            daily["medication"] = Pair(medsConfirmed, medsTotal)

            // Hydration
            val hydrationLogs = db.collection("hydration_logs").whereEqualTo("patient_id", patientId).get().await()
            val hydrationToday = hydrationLogs.documents.filter {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            val hydrationConfirmed = hydrationToday.count { (it.getString("status") ?: "") == "confirmed" }
            val hydrationTotal = hydrationToday.size
            daily["hydration"] = Pair(hydrationConfirmed, hydrationTotal)

            // Nutrition
            val nutritionLogs = db.collection("nutrition_logs").whereEqualTo("patient_id", patientId).get().await()
            val nutritionToday = nutritionLogs.documents.filter {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            val nutritionConfirmed = nutritionToday.count { (it.getString("status") ?: "") == "confirmed" }
            val nutritionTotal = nutritionToday.size
            daily["nutrition"] = Pair(nutritionConfirmed, nutritionTotal)

            dailyStats = daily

            // --- Monthly ---
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
        } else {
            dailyStats = emptyMap()
            monthlyStats = emptyMap()
        }
    }

    LaunchedEffect(user) {
        fetchLinkedPatients()
    }

    // Update stats whenever selectedPatient changes
    LaunchedEffect(selectedPatient) {
        coroutineScope.launch {
            fetchStatsForPatient(selectedPatient)
        }
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            FamilyBottomBar(
                selectedItem = bottomNavSelected,
                onItemSelected = { item ->
                    bottomNavSelected = item
                    when (item) {
                        "dashboard" -> navController.navigate(Routes.FAMILY) { launchSingleTop = true }
                        "patients" -> navController.navigate(Routes.FAMILY_PATIENTS) { launchSingleTop = true }
                        "alerts" -> navController.navigate(Routes.FAMILY_ALERTS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.FAMILY_PROFILE) { launchSingleTop = true }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Greeting and date
            Text("Olá, $familyName!", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(todayFormatted, color = Color.Gray, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            // Link Patient Button
            Button(
                onClick = { linkDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF284545),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Link Patient by Email")
            }
            Spacer(Modifier.height(20.dp))

            // Patient List (larger cards, image + name)
            if (patients.isNotEmpty()) {
                Text("Linked Patients", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    patients.forEach { patient ->
                        val isSelected = selectedPatient?.get("uid") == patient["uid"]
                        Card(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .width(160.dp)
                                .height(210.dp)
                                .clickable { selectedPatient = patient },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFB3E5FC) else Color(0xFFF5F5F5)
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                val imageUrl = patient["profileImage"] as? String
                                if (imageUrl != null && imageUrl.startsWith("http")) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                            .background(Color(0xFFE0F7FA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp),
                                            tint = Color(0xFF0288D1)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(0.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(bottom = 8.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        patient["name"] as? String ?: "Unnamed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Patient dropdown to select
            if (patients.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.get("name") as? String ?: "Select Patient",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Select Patient") }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient["name"] as? String ?: "Unnamed") },
                                onClick = {
                                    selectedPatient = patient
                                    expanded = false
                                    // Fetch stats for the newly selected patient
                                    coroutineScope.launch {
                                        fetchStatsForPatient(patient)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 2x2 grid for stats (now dynamic)
            if (selectedPatient != null) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val checkin = dailyStats["checkin"] as? Pair<*, *> ?: Pair(false, false)
                        val checkinCount = listOf(checkin.first, checkin.second).count { it == true }
                        StatCardWithIcon(
                            "Check-in",
                            checkinCount,
                            2,
                            Icons.Default.CheckCircle,
                            Color(0xFFB2DFDB),
                            modifier = Modifier.weight(1f)
                        )
                        val med = dailyStats["medication"] as? Pair<Int, Int> ?: Pair(0, 0)
                        StatCardWithIcon(
                            "Medication",
                            med.first,
                            med.second,
                            Icons.Default.MedicalServices,
                            Color(0xFFFFF9C4),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val hydration = dailyStats["hydration"] as? Pair<Int, Int> ?: Pair(0, 0)
                        StatCardWithIcon(
                            "Hydration",
                            hydration.first,
                            hydration.second,
                            Icons.Default.LocalDrink,
                            Color(0xFFBBDEFB),
                            modifier = Modifier.weight(1f)
                        )
                        val nutrition = dailyStats["nutrition"] as? Pair<Int, Int> ?: Pair(0, 0)
                        StatCardWithIcon(
                            "Nutrition",
                            nutrition.first,
                            nutrition.second,
                            Icons.Default.Restaurant,
                            Color(0xFFFFCCBC),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Link Patient Dialog
    if (linkDialogOpen) {
        AlertDialog(
            onDismissRequest = { linkDialogOpen = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF284545),
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        "Link Patient by Email",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "Enter the patient's email address to link them to your account.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                        lineHeight = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = linkEmail,
                        onValueChange = { linkEmail = it },
                        label = { Text("Patient Email") },
                        singleLine = true,
                        isError = linkError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (linkError != null) {
                        Text(linkError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        linking = true
                        linkError = null
                        db.collection("users").whereEqualTo("email", linkEmail).whereEqualTo("role", "older_adult").get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val patientId = querySnapshot.documents[0].id
                                    // --- Check if already linked ---
                                    db.collection("family_patients")
                                        .whereEqualTo("family_id", user!!.uid)
                                        .whereEqualTo("patient_id", patientId)
                                        .get()
                                        .addOnSuccessListener { linkSnapshot ->
                                            if (!linkSnapshot.isEmpty) {
                                                linkError = "This patient is already linked to your account."
                                                linking = false
                                            } else {
                                                db.collection("family_patients").add(
                                                    mapOf(
                                                        "family_id" to user!!.uid,
                                                        "patient_id" to patientId
                                                    )
                                                ).addOnSuccessListener {
                                                    Toast.makeText(context, "Patient linked! (id: $patientId)", Toast.LENGTH_LONG).show()
                                                    linkEmail = ""
                                                    linkDialogOpen = false
                                                    coroutineScope.launch {
                                                        fetchLinkedPatients()
                                                    }
                                                }.addOnFailureListener { e ->
                                                    linkError = "Failed to link: ${e.localizedMessage}"
                                                }
                                                linking = false
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            linkError = "Error: ${e.localizedMessage}"
                                            linking = false
                                        }
                                } else {
                                    linkError = "No patient found with that email."
                                    linking = false
                                }
                            }
                            .addOnFailureListener { e ->
                                linkError = "Error: ${e.localizedMessage}"
                                linking = false
                            }
                    },
                    enabled = !linking && linkEmail.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF284545),
                        contentColor = Color.White
                    )
                ) {
                    Text("Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { linkDialogOpen = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun StatCardWithIcon(label: String, value: Int, total: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF284545), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("$value out of $total", fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, softWrap = true, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
