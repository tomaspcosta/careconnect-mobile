package com.example.careconnect.ui.screens.caregiver

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
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
import com.example.careconnect.ui.components.bottomnav.CaregiverBottomBar
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
fun CaregiverDashboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser
    var patients by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedPatient by remember { mutableStateOf<Map<String, Any>?>(null) }
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var linkDialogOpen by remember { mutableStateOf(false) }
    var linkEmail by remember { mutableStateOf("") }
    var linking by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var bottomNavSelected by remember { mutableStateOf("dashboard") }
    var caregiverName by remember { mutableStateOf("") }
    // Fetch caregiver name
    LaunchedEffect(user) {
        if (user != null) {
            val doc = db.collection("users").document(user.uid).get().await()
            caregiverName = doc.getString("name") ?: "Caregiver"
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

    // Fetch linked patients using join collection
    suspend fun fetchLinkedPatients() {
        if (user != null) {
            try {
                val caregiverId = user.uid
                val links = db.collection("caregiver_patients")
                    .whereEqualTo("caregiver_id", caregiverId)
                    .get().await()
                val patientIds = links.documents.mapNotNull { it.getString("patient_id") }.toSet() // prevent duplicates
                val patientsList = mutableListOf<Map<String, Any>>()
                for (pid in patientIds) {
                    val patientDoc = db.collection("users").document(pid).get().await()
                    val data = patientDoc.data
                    if (data != null) {
                        patientsList.add(data + mapOf("uid" to pid))
                    }
                }
                patients = patientsList
                if (selectedPatient == null && patientsList.isNotEmpty()) {
                    selectedPatient = patientsList[0]
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                patients = emptyList()
                selectedPatient = null
            }
        }
    }

    LaunchedEffect(user) {
        fetchLinkedPatients()
    }

    // Fetch stats for selected patient (count logs for today)
    LaunchedEffect(selectedPatient) {
        if (selectedPatient != null) {
            val patientId = selectedPatient!!["uid"] as String
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val statMap = mutableMapOf<String, Int>()
            // Check-in
            val checkinLogs = db.collection("checkin_logs").whereEqualTo("patient_id", patientId).get().await()
            statMap["checkin"] = checkinLogs.documents.count {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            // Medication
            val medLogs = db.collection("tasks").whereEqualTo("patient_id", patientId).whereEqualTo("date", today).get().await()
            statMap["medication"] = medLogs.documents.count {
                (it.getString("name")?.lowercase()?.contains("medication") == true)
            }
            // Hydration
            val hydrationLogs = db.collection("hydration_logs").whereEqualTo("patient_id", patientId).get().await()
            statMap["hydration"] = hydrationLogs.documents.count {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            // Nutrition
            val nutritionLogs = db.collection("nutrition_logs").whereEqualTo("patient_id", patientId).get().await()
            statMap["nutrition"] = nutritionLogs.documents.count {
                val ts = it.getTimestamp("timestamp")?.toDate()
                ts != null && SimpleDateFormat("yyyy-MM-dd").format(ts) == today
            }
            stats = statMap
        } else {
            stats = emptyMap()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            CaregiverBottomBar(
                selectedItem = bottomNavSelected,
                onItemSelected = { item ->
                    bottomNavSelected = item
                    when (item) {
                        "dashboard" -> navController.navigate(Routes.CAREGIVER) { launchSingleTop = true }
                        "patients" -> navController.navigate(Routes.CAREGIVER_PATIENTS) { launchSingleTop = true }
                        "alerts" -> navController.navigate(Routes.CAREGIVER_ALERTS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.CAREGIVER_PROFILE) { launchSingleTop = true }
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
            Text("Olá, $caregiverName!", fontWeight = FontWeight.Bold, fontSize = 22.sp)
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
                                Spacer(Modifier.height(0.dp)) // Remove espaço extra
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
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 2x2 grid for stats
            if (selectedPatient != null) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCardWithIcon("Check-in", stats["checkin"] ?: 0, 3, Icons.Default.CheckCircle, Color(0xFFB2DFDB), modifier = Modifier.weight(1f))
                        StatCardWithIcon("Medication", stats["medication"] ?: 0, 3, Icons.Default.MedicalServices, Color(0xFFFFF9C4), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCardWithIcon("Hydration", stats["hydration"] ?: 0, 3, Icons.Default.LocalDrink, Color(0xFFBBDEFB), modifier = Modifier.weight(1f))
                        StatCardWithIcon("Nutrition", stats["nutrition"] ?: 0, 3, Icons.Default.Restaurant, Color(0xFFFFCCBC), modifier = Modifier.weight(1f))
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
                                    // Only allow linking if user is "older_adult"
                                    val userRole = querySnapshot.documents[0].getString("role")
                                    if (userRole != "older_adult") {
                                        linkError = "Only older adult emails are allowed."
                                        linking = false
                                        return@addOnSuccessListener
                                    }
                                    // Check if already linked
                                    db.collection("caregiver_patients")
                                        .whereEqualTo("caregiver_id", user!!.uid)
                                        .whereEqualTo("patient_id", patientId)
                                        .get()
                                        .addOnSuccessListener { linkSnapshot ->
                                            if (!linkSnapshot.isEmpty) {
                                                linkError = "This patient is already linked to your account."
                                                linking = false
                                            } else {
                                                db.collection("caregiver_patients").add(
                                                    mapOf(
                                                        "caregiver_id" to user!!.uid,
                                                        "patient_id" to patientId
                                                    )
                                                ).addOnSuccessListener {
                                                    Toast.makeText(context, "Patient linked! (id: $patientId)", Toast.LENGTH_LONG).show()
                                                    linkEmail = ""
                                                    linkDialogOpen = false
                                                    // Refresh patients list after linking
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
