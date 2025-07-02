package com.example.careconnect.ui.screens.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.R
import com.example.careconnect.ui.components.bottomnav.FamilyBottomBar
import com.example.careconnect.ui.navigation.Routes
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import coil.compose.AsyncImage
import kotlinx.coroutines.tasks.await

data class PatientListItem(
    val id: Int,
    val name: String,
    val phone: String,
    val imageResId: Int
)

@Composable
fun FamilyPatientsScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("patients") }
    var searchQuery by remember { mutableStateOf("") }
    var patients by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var loading by remember { mutableStateOf(true) }
    var unlinkDialogOpen by remember { mutableStateOf<String?>(null) }
    var pendingRemovePatient by remember { mutableStateOf<Map<String, Any>?>(null) }
    // Remove dialog state for medication/task
    val context = LocalContext.current
    val db = Firebase.firestore
    val user = Firebase.auth.currentUser

    // Fetch linked patients from Firestore
    LaunchedEffect(user) {
        if (user != null) {
            loading = true
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
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                patients = emptyList()
            }
            loading = false
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
                        "patients" -> {}
                        "alerts" -> navController.navigate(Routes.FAMILY_ALERTS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.FAMILY_PROFILE) { launchSingleTop = true }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "My Patients",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF284545),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Manage the older adults under your care",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            // --- Remove Add Buttons Row ---

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                placeholder = { Text("Search by name or email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredPatients = patients.filter {
                    val name = (it["name"] as? String)?.lowercase() ?: ""
                    val email = (it["email"] as? String)?.lowercase() ?: ""
                    val query = searchQuery.lowercase()
                    name.contains(query) || email.contains(query)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPatients) { patient ->
                        PatientCardGridFirestore(
                            patient = patient,
                            onView = {
                                val uid = patient["uid"] as? String
                                if (uid != null) {
                                    navController.navigate("family_old_adult/$uid")
                                }
                            },
                            onRemove = {
                                unlinkDialogOpen = patient["uid"] as String
                                pendingRemovePatient = patient
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Remove Medication and Task Dialogs ---

    if (unlinkDialogOpen != null && pendingRemovePatient != null) {
        AlertDialog(
            onDismissRequest = { unlinkDialogOpen = null; pendingRemovePatient = null },
            title = { Text("Unlink Patient", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to unlink this patient?") },
            confirmButton = {
                Button(
                    onClick = {
                        val familyId = user?.uid
                        val patientId = unlinkDialogOpen
                        db.collection("family_patients")
                            .whereEqualTo("family_id", familyId)
                            .whereEqualTo("patient_id", patientId)
                            .get()
                            .addOnSuccessListener { docs ->
                                for (doc in docs) {
                                    db.collection("family_patients").document(doc.id).delete()
                                }
                                Toast.makeText(context, "Patient removed", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        patients = patients.filter { it["uid"] != patientId }
                        unlinkDialogOpen = null
                        pendingRemovePatient = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Unlink", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { unlinkDialogOpen = null; pendingRemovePatient = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PatientCardGridFirestore(
    patient: Map<String, Any>,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            val imageUrl = patient["profileImage"] as? String
            if (imageUrl != null && imageUrl.startsWith("http")) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = patient["name"] as? String ?: "Profile",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFFE0F7FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFF284545)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = patient["name"] as? String ?: "Unnamed",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onView,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View Patient",
                        tint = Color(0xFF284545),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoAccounts,
                        contentDescription = "Unlink Patient",
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
