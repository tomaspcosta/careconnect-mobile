package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.careconnect.R
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class CaregiverModel(
    val uid: String,
    val name: String,
    val phone: String,
    val role: String,
    val imageUrl: String? = null
)


class CaregiverViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _caregivers = MutableStateFlow<List<CaregiverModel>>(emptyList())
    val caregivers = _caregivers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var patientId: String? = null

    init {
        viewModelScope.launch {
            loadPatientIdAndFetchCaregivers()
        }
    }

    private suspend fun loadPatientIdAndFetchCaregivers() {
        _isLoading.value = true
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            try {
                val userQuery = db.collection("users")
                    .whereEqualTo("email", currentUser.email)
                    .limit(1)
                    .get()
                    .await()

                if (userQuery.isEmpty) {
                    Log.e("CaregiverViewModel", "Patient document not found for email: ${currentUser.email}")
                    _isLoading.value = false
                    return
                }

                val userDoc = userQuery.documents.first()
                patientId = userDoc.getString("uid")

                if (patientId != null) {
                    fetchCaregivers()
                } else {
                    Log.e("CaregiverViewModel", "'uid' field not found in patient document")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CaregiverViewModel", "Error loading patient ID", e)
                _isLoading.value = false
            }
        } else {
            Log.e("CaregiverViewModel", "No authenticated user or email found")
            _isLoading.value = false
        }
    }


    private fun fetchCaregivers() {
        if (patientId == null) {
            _isLoading.value = false
            return
        }
        _isLoading.value = true

        db.collection("caregiver_patients")
            .whereEqualTo("patient_id", patientId)
            .addSnapshotListener { relationsSnapshot, error ->
                if (error != null) {
                    Log.e("CaregiverViewModel", "Error fetching caregiver relations", error)
                    return@addSnapshotListener
                }

                val caregiverIds = relationsSnapshot?.documents?.mapNotNull { it.getString("caregiver_id") } ?: emptyList()

                db.collection("family_patients")
                    .whereEqualTo("patient_id", patientId)
                    .addSnapshotListener { familyRelationsSnapshot, familyError ->
                        if (familyError != null) {
                            Log.e("CaregiverViewModel", "Error fetching family relations", familyError)
                            _isLoading.value = false
                            return@addSnapshotListener
                        }

                        val familyIds = familyRelationsSnapshot?.documents?.mapNotNull { it.getString("caregiver_id") } ?: emptyList()
                        val allIds = (caregiverIds + familyIds).distinct()

                        if (allIds.isNotEmpty()) {
                            db.collection("users").whereIn(FieldPath.documentId(), allIds)
                                .addSnapshotListener { usersSnapshot, usersError ->
                                    if (usersError != null) {
                                        Log.e("CaregiverViewModel", "Error fetching user details", usersError)
                                        _isLoading.value = false
                                        return@addSnapshotListener
                                    }
                                    _caregivers.value = usersSnapshot?.documents?.mapNotNull { doc ->
                                        CaregiverModel(
                                            uid = doc.id,
                                            name = doc.getString("name") ?: "Unknown",
                                            phone = doc.getString("phone") ?: "No Phone",
                                            role = doc.getString("role") ?: "caregiver",
                                            imageUrl = doc.getString("profileImage")
                                        )
                                    } ?: emptyList()
                                    _isLoading.value = false
                                }
                        } else {
                            _caregivers.value = emptyList()
                            _isLoading.value = false
                        }
                    }
            }
    }

    fun addCaregiver(email: String, role: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (patientId == null) {
            onError("Current user not found.")
            return
        }
        viewModelScope.launch {
            try {
                val userQuery = db.collection("users").whereEqualTo("email", email).limit(1).get().await()
                if (userQuery.isEmpty) {
                    onError("No user found with email '$email'")
                    return@launch
                }

                val caregiverDoc = userQuery.documents.first()
                val caregiverId = caregiverDoc.id
                val userRoleOnProfile = caregiverDoc.getString("role")


                if (userRoleOnProfile != "caregiver" && userRoleOnProfile != "family") {
                    onError("The specified user is not a caregiver or family member.")
                    return@launch
                }

                val isAlreadyCaregiver = !db.collection("caregiver_patients")
                    .whereEqualTo("patient_id", patientId)
                    .whereEqualTo("caregiver_id", caregiverId)
                    .limit(1).get().await().isEmpty

                val isAlreadyFamily = !db.collection("family_patients")
                    .whereEqualTo("patient_id", patientId)
                    .whereEqualTo("caregiver_id", caregiverId)
                    .limit(1).get().await().isEmpty

                if (isAlreadyCaregiver || isAlreadyFamily) {
                    onError("This user is already linked.")
                    return@launch
                }

                // Determine the correct collection based on the 'role' parameter from the dialog
                val collectionName = if (role == "family") "family_patients" else "caregiver_patients"
                val relation = hashMapOf("patient_id" to patientId, "caregiver_id" to caregiverId)
                db.collection(collectionName).add(relation).await()
                onSuccess("Successfully added $email")

            } catch (e: Exception) {
                Log.e("CaregiverViewModel", "Error adding connection", e)
                onError("An error occurred: ${e.message}")
            }
        }
    }

    fun removeCaregiver(caregiverId: String, role: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (patientId == null) {
            onError("Current user not found.")
            return
        }
        viewModelScope.launch {
            try {
                val collectionName = if (role == "family") "family_patients" else "caregiver_patients"

                val relationsQuery = db.collection(collectionName)
                    .whereEqualTo("patient_id", patientId)
                    .whereEqualTo("caregiver_id", caregiverId)
                    .get().await()

                if (relationsQuery.documents.isNotEmpty()) {
                    for (document in relationsQuery.documents) {
                        db.collection(collectionName).document(document.id).delete().await()
                    }
                    onSuccess("Connection removed.")
                } else {
                    // Fallback: Check the other collection just in case
                    val otherCollection = if (role == "family") "caregiver_patients" else "family_patients"
                    val otherRelationsQuery = db.collection(otherCollection)
                        .whereEqualTo("patient_id", patientId)
                        .whereEqualTo("caregiver_id", caregiverId)
                        .get().await()

                    if (otherRelationsQuery.documents.isNotEmpty()) {
                        for (document in otherRelationsQuery.documents) {
                            db.collection(otherCollection).document(document.id).delete().await()
                        }
                        onSuccess("Connection removed.")
                    } else {
                        Log.w("CaregiverViewModel", "No relation found to remove for caregiverId: $caregiverId")
                        onError("Connection not found.")
                    }
                }
            } catch (e: Exception) {
                Log.e("CaregiverViewModel", "Error removing connection", e)
                onError("An error occurred: ${e.message}")
            }
        }
    }
}


@Composable
fun CaregiversScreen(
    navController: NavHostController,
    viewModel: CaregiverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("caregivers") }
    var selectedTab by remember { mutableStateOf("family") }
    var showAddDialogForRole by remember { mutableStateOf<String?>(null) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<CaregiverModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val caregiversState by viewModel.caregivers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredList = remember(caregiversState, selectedTab, searchQuery) {
        caregiversState.filter {
            it.role.equals(selectedTab, ignoreCase = true) &&
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }

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
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My Care Team", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF284545))
            Text(
                "Manage your connections with caregivers and family",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search icon") },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showTypeDialog = true },
                    modifier = Modifier.size(52.dp).background(Color(0xFF284545), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TabButton(
                    text = "Family",
                    isSelected = selectedTab == "family",
                    modifier = Modifier.weight(1f)
                ) { selectedTab = "family" }

                Spacer(modifier = Modifier.width(8.dp))

                TabButton(
                    text = "Caregivers",
                    isSelected = selectedTab == "caregiver",
                    modifier = Modifier.weight(1f)
                ) { selectedTab = "caregiver" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredList.isEmpty()) {
                Text(
                    "No ${selectedTab}s found.",
                    modifier = Modifier.padding(top = 32.dp),
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredList, key = { it.uid }) { contact ->
                        CaregiverCard(
                            contact = contact,
                            onDelete = {
                                contactToDelete = contact
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTypeDialog) {
        AddTypeDialog(
            onDismiss = { showTypeDialog = false },
            onSelect = { role ->
                showTypeDialog = false
                showAddDialogForRole = role
            }
        )
    }

    if (showAddDialogForRole != null) {
        AddCaregiverDialog(
            role = showAddDialogForRole!!,
            onDismiss = { showAddDialogForRole = null },
            onAdd = { email ->
                viewModel.addCaregiver(email, showAddDialogForRole!!,
                    onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
                showAddDialogForRole = null
            }
        )
    }

    if (showDeleteDialog && contactToDelete != null) {
        DeleteConfirmationDialog(
            contactName = contactToDelete!!.name,
            onConfirm = {
                viewModel.removeCaregiver(contactToDelete!!.uid, contactToDelete!!.role,
                    onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
                showDeleteDialog = false
                contactToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                contactToDelete = null
            }
        )
    }
}

@Composable
fun AddTypeDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Connection") },
        text = { Text("Select the type of connection you want to add.") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onSelect("caregiver") }) { Text("Caregiver") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { onSelect("family") }) { Text("Family") }
            }
        },
        dismissButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}


@Composable
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF284545) else Color(0xFFD3D3D3),
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
fun CaregiverCard(contact: CaregiverModel, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                model = contact.imageUrl,
                contentDescription = contact.name,
                placeholder = painterResource(id = R.drawable.default_profile),
                error = painterResource(id = R.drawable.default_profile),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF284545))
                Text(contact.phone, color = Color.DarkGray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Remove", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddCaregiverDialog(role: String, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${role.replaceFirstChar { it.uppercase() }}") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter user's email") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(email) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(contactName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to remove $contactName?") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
