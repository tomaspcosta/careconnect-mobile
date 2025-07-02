package com.example.careconnect.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.AdminBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val last_login: Timestamp? = null,
    val profileImage: String = ""
)

@Composable
fun UserManagementScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("users") }

    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var displayedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("all") }

    val firestore = Firebase.firestore
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val fetchUsers: () -> Unit = {
        isLoading = true
        firestore.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val fetchedList = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        user?.copy(uid = doc.id)
                    } catch (e: Exception) {
                        Log.e("UserManagementScreen", "Error parsing user document ${doc.id}: ${e.message}")
                        null
                    }
                }.filter { it.role.isNotEmpty() }
                allUsers = fetchedList
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("UserManagementScreen", "Error fetching users: ${e.message}", e)
                isLoading = false
                coroutineScope.launch { snackbarHostState.showSnackbar("Error fetching users: ${e.message}") }
            }
    }

    val applyFilters: () -> Unit = {
        val query = searchQuery.lowercase(Locale.ROOT).trim()
        displayedUsers = allUsers.filter { user ->
            val nameMatches = user.name.lowercase(Locale.ROOT).contains(query)
            val emailMatches = user.email.lowercase(Locale.ROOT).contains(query)
            val roleMatches = selectedRoleFilter == "all" || user.role.lowercase(Locale.ROOT) == selectedRoleFilter.lowercase(Locale.ROOT)
            (nameMatches || emailMatches) && roleMatches
        }
    }

    val onDeleteUserConfirmed: (User) -> Unit = { userToDelete ->
        coroutineScope.launch {
            firestore.collection("users").document(userToDelete.uid).delete()
                .addOnSuccessListener {
                    Log.d("UserManagementScreen", "User ${userToDelete.name} deleted from Firestore.")
                    fetchUsers()
                    coroutineScope.launch { snackbarHostState.showSnackbar("User deleted successfully!") }
                }
                .addOnFailureListener { e ->
                    Log.e("UserManagementScreen", "Error deleting user ${userToDelete.name}: ${e.message}", e)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Failed to delete user: ${e.message}") }
                }
        }
    }

    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var createAdminName by remember { mutableStateOf("") }
    var createAdminEmail by remember { mutableStateOf("") }
    var createAdminPhone by remember { mutableStateOf("") }
    var createAdminPassword by remember { mutableStateOf("") }
    var createAdminLoading by remember { mutableStateOf(false) }
    var createAdminError by remember { mutableStateOf<String?>(null) }

    val onCreateAdmin: () -> Unit = {
        createAdminLoading = true
        createAdminError = null
        // Create admin user in Firebase Auth and Firestore
        val auth = Firebase.auth
        auth.createUserWithEmailAndPassword(createAdminEmail.trim(), createAdminPassword)
            .addOnSuccessListener { result ->
                val user = result.user
                user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(createAdminName.trim())
                        .build()
                )
                val userData = hashMapOf(
                    "name" to createAdminName.trim(),
                    "email" to createAdminEmail.trim(),
                    "phone" to createAdminPhone.trim(),
                    "role" to "admin",
                    "profileImage" to "",
                    "last_login" to null
                )
                firestore.collection("users").document(user!!.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        createAdminLoading = false
                        showCreateAdminDialog = false
                        createAdminName = ""
                        createAdminEmail = ""
                        createAdminPhone = ""
                        createAdminPassword = ""
                        coroutineScope.launch { snackbarHostState.showSnackbar("Admin user created!") }
                        fetchUsers()
                    }
                    .addOnFailureListener { e ->
                        createAdminLoading = false
                        createAdminError = "Failed to save user: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                createAdminLoading = false
                createAdminError = "Failed to create user: ${e.message}"
            }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    LaunchedEffect(searchQuery, selectedRoleFilter, allUsers) {
        applyFilters()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AdminBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item -> // Renamed 'it' to 'item' for clarity
                    when (item) {
                        "dashboard" -> {
                            navController.navigate(Routes.ADMIN) {
                                popUpTo(Routes.ADMIN) { inclusive = true } // Adjusted for consistency
                                launchSingleTop = true
                            }
                        }
                        "users" -> {
                            navController.navigate(Routes.ADMIN_USERS) {
                                popUpTo(Routes.ADMIN_USERS) { inclusive = true } // Adjusted for consistency
                                launchSingleTop = true
                            }
                        }
                        "profile" -> {
                            navController.navigate(Routes.ADMIN_PROFILE) {
                                popUpTo(Routes.ADMIN) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    selectedItem = item // Moved this line to after navigation logic
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "System User List",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF284545),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Manage the users\nregistered in the system",
                fontSize = 14.sp,
                color = Color(0xFF284545),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            // Move Create Admin Button here, centered
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showCreateAdminDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Admin", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Create Admin Dialog
            if (showCreateAdminDialog) {
                var passwordVisible by remember { mutableStateOf(false) }
                val isEmailValid = createAdminEmail.contains("@") && createAdminEmail.contains(".")
                val isPasswordValid = createAdminPassword.length >= 6
                val isNameValid = createAdminName.trim().length > 2

                AlertDialog(
                    onDismissRequest = {
                        showCreateAdminDialog = false
                        createAdminName = ""
                        createAdminEmail = ""
                        createAdminPhone = ""
                        createAdminPassword = ""
                        createAdminError = null
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFFE91E63), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Create Admin User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFFE91E63),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .background(Color(0xFFF8F8F8), shape = RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = createAdminName,
                                onValueChange = { createAdminName = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("e.g. Jane Doe") },
                                singleLine = true,
                                isError = !isNameValid && createAdminName.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isNameValid && createAdminName.isNotEmpty()) {
                                Text("Enter at least 3 characters.", color = Color.Red, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = createAdminEmail,
                                onValueChange = { createAdminEmail = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. admin@email.com") },
                                singleLine = true,
                                isError = !isEmailValid && createAdminEmail.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isEmailValid && createAdminEmail.isNotEmpty()) {
                                Text("Enter a valid email address.", color = Color.Red, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = createAdminPhone,
                                onValueChange = { createAdminPhone = it },
                                label = { Text("Phone Number (optional)") },
                                placeholder = { Text("e.g. +1234567890") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = createAdminPassword,
                                onValueChange = { createAdminPassword = it },
                                label = { Text("Password") },
                                placeholder = { Text("At least 6 characters") },
                                singleLine = true,
                                isError = !isPasswordValid && createAdminPassword.isNotEmpty(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isPasswordValid && createAdminPassword.isNotEmpty()) {
                                Text("Password must be at least 6 characters.", color = Color.Red, fontSize = 12.sp)
                            }
                            if (createAdminError != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = createAdminError ?: "",
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onCreateAdmin() },
                            enabled = isNameValid && isEmailValid && isPasswordValid && !createAdminLoading
                        ) {
                            if (createAdminLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Create", color = if (isNameValid && isEmailValid && isPasswordValid) Color(0xFFE91E63) else Color.Gray)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showCreateAdminDialog = false
                                createAdminName = ""
                                createAdminEmail = ""
                                createAdminPhone = ""
                                createAdminPassword = ""
                                createAdminError = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or email") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                val roles = listOf("all", "admin", "caregiver", "family", "older_adult")

                // Define icon and color mapping for the dropdown filter
                val roleDisplayInfo = mapOf(
                    "all" to Triple("All Roles", Icons.Default.FilterList, Color.Gray),
                    "admin" to Triple("Admin", Icons.Default.AdminPanelSettings, Color(0xFFE91E63)),
                    "caregiver" to Triple("Caregiver", Icons.Default.MedicalServices, Color(0xFFFF9680)),
                    "family" to Triple("Family Member", Icons.Default.FamilyRestroom, Color(0xFF40CFC2)),
                    "older_adult" to Triple("Old Adult", Icons.Default.Elderly, Color(0xFF8A63E9))
                )

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by role")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { role ->
                            val (displayText, icon, color) = roleDisplayInfo[role] ?: Triple(role, Icons.Default.Person, Color.Black) // Fallback
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = displayText)
                                    }
                                },
                                onClick = {
                                    selectedRoleFilter = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found matching your criteria.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayedUsers, key = { it.uid }) { user ->
                        UserCard(user = user, currentUserId = currentUserId, onDeleteClick = onDeleteUserConfirmed)
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, currentUserId: String?, onDeleteClick: (User) -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var enteredEmail by remember { mutableStateOf("") }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm User Deletion") },
            text = {
                Column {
                    Text("To confirm deletion of ${user.name}'s account, please type their email address below:")
                    Text(
                        text = user.email,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = enteredEmail,
                        onValueChange = { enteredEmail = it },
                        label = { Text("Enter Email") },
                        isError = enteredEmail.isNotEmpty() && enteredEmail != user.email,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (enteredEmail.isNotEmpty() && enteredEmail != user.email) {
                        Text(
                            text = "Email does not match.",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteClick(user)
                        enteredEmail = ""
                    },
                    enabled = enteredEmail == user.email
                ) {
                    Text("Delete", color = if (enteredEmail == user.email) Color.Red else Color.Gray)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    enteredEmail = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImage.isNotEmpty()) {
                        AsyncImage(
                            model = user.profileImage,
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.Bold)
                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                    if (user.phone.isNotEmpty()) {
                        Text(user.phone, fontSize = 12.sp, color = Color.Gray)
                    }
                    val lastLoginDate = user.last_login?.toDate()
                    val formattedLastLogin = lastLoginDate?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "-"
                    Text("Last Login: $formattedLastLogin", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (roleIcon, roleColor) = when (user.role.lowercase(Locale.ROOT)) {
                    "older_adult" -> Pair(Icons.Default.Elderly, Color(0xFF8A63E9))
                    "caregiver" -> Pair(Icons.Default.MedicalServices, Color(0xFFFF9680))
                    "family" -> Pair(Icons.Default.FamilyRestroom, Color(0xFF40CFC2))
                    "admin" -> Pair(Icons.Default.AdminPanelSettings, Color(0xFFE91E63))
                    else -> Pair(Icons.Default.Person, Color.Gray)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = roleColor,
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (user.role.lowercase(Locale.ROOT)) {
                            "older_adult" -> "Old Adult"
                            "caregiver" -> "Caregiver"
                            "family" -> "Family Member"
                            "admin" -> "Admin"
                            else -> "Unknown"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (user.uid != currentUserId) {
                    TextButton(
                        onClick = {
                            showConfirmDialog = true
                            enteredEmail = ""
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                    ) {
                        Text("Delete Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

