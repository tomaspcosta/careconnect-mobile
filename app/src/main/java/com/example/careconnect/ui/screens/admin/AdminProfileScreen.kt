package com.example.careconnect.ui.screens.admin // Changed package to admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Import all filled icons for convenience
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.AdminBottomBar // Changed to AdminBottomBar
import com.example.careconnect.ui.navigation.Routes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

@Composable
fun AdminProfileScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+351") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var uploadingImage by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }

    // Password dialog fields
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    var selectedItem by remember { mutableStateOf("profile") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadingImage = true
            uploadProfileImageToFirebase(uri) { url ->
                profileImageUrl = url
                uploadingImage = false
            }
        }
    }

    // Load user data from Firestore
    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            email = user.email ?: ""
            val doc = Firebase.firestore.collection("users").document(user.uid).get().await()
            val data = doc.data
            if (data != null) {
                fullName = data.get("name") as? String ?: ""
                // Removed loading of address and dob
                val phoneDb = data.get("phone") as? String ?: ""
                // Separa country code e número (assuming it's stored like "+CC NNNNNNNNN")
                val phoneParts = phoneDb.trim().split(" ", limit = 2)
                countryCode = if (phoneParts.isNotEmpty() && phoneParts[0].startsWith("+")) phoneParts[0] else "+351"
                phoneNumber = if (phoneParts.size > 1) phoneParts[1] else phoneDb // If no code, keep full number
                profileImageUrl = data.get("profileImage") as? String
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AdminBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when(item) {
                        "dashboard" -> navController.navigate(Routes.ADMIN) { launchSingleTop = true }
                        "users" -> navController.navigate(Routes.ADMIN_USERS) { launchSingleTop = true }
                        "profile" -> { /* Already here */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture with camera icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F4FD)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Image",
                            modifier = Modifier.size(120.dp),
                            tint = Color(0xFF284545)
                        )
                    }
                    if (uploadingImage) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                // Camera icon for upload
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 32.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload",
                        tint = Color(0xFF284545),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = fullName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = "Admin", color = Color.Gray, fontSize = 16.sp) // Always "Admin" for AdminProfileScreen

            Spacer(modifier = Modifier.height(24.dp))

            // Email (read only)
            CustomTextField(
                value = email,
                onValueChange = { /* no-op */ },
                label = "Email",
                icon = Icons.Default.Email,
                readOnly = true,
                textColor = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.height(60.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Full Name (editable)
            CustomTextField(
                value = fullName,
                onValueChange = { if (isEditingName) fullName = it },
                label = "Full Name",
                icon = Icons.Default.Person,
                readOnly = !isEditingName,
                textColor = if (!isEditingName) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545),
                modifier = Modifier.height(60.dp),
                trailingIcon = {
                    IconButton(onClick = { isEditingName = !isEditingName }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = if (isEditingName) Color(0xFF284545) else Color.Gray
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it },
                    label = { Text("Country Code") },
                    modifier = Modifier
                        .width(110.dp),
                    readOnly = !isEditingPhone,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF284545),
                        unfocusedBorderColor = Color(0xFFE1E8ED),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = if (!isEditingPhone) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545),
                        unfocusedTextColor = if (!isEditingPhone) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (isEditingPhone) phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone Number",
                            tint = Color(0xFF284545),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isEditingPhone = !isEditingPhone }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Phone",
                                tint = if (isEditingPhone) Color(0xFF284545) else Color.Gray
                            )
                        }
                    },
                    readOnly = !isEditingPhone,
                    textStyle = LocalTextStyle.current.copy(color = if (!isEditingPhone) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545)),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF284545),
                        unfocusedBorderColor = Color(0xFFE1E8ED),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = if (!isEditingPhone) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545),
                        unfocusedTextColor = if (!isEditingPhone) Color.Gray.copy(alpha = 0.7f) else Color(0xFF284545)
                    ),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Change Password", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val user = Firebase.auth.currentUser
                    if (user != null) {
                        val updateMap = mutableMapOf<String, Any>(
                            "name" to fullName,
                            // Removed "dob" from update map
                            "phone" to (countryCode.trim() + " " + phoneNumber.trim()),
                            "countryCode" to countryCode
                        )
                        // Removed "address" from update map
                        Firebase.firestore.collection("users").document(user.uid).set(
                            updateMap,
                            com.google.firebase.firestore.SetOptions.merge()
                        ).addOnSuccessListener {
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                            isEditingName = false
                            isEditingPhone = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Update Profile", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LANDING) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Logout", color = Color.White)
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = {
                showChangePasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                passwordError = null
            },
            onChangePassword = { newPassword ->
                showChangePasswordDialog = false
                Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
            },
            currentPassword = currentPassword,
            onCurrentPasswordChange = { currentPassword = it },
            newPassword = newPassword,
            onNewPasswordChange = { newPassword = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            passwordError = passwordError,
            setPasswordError = { passwordError = it }
        )
    }
}

fun uploadProfileImageToFirebase(uri: Uri, onResult: (String?) -> Unit) {
    val user = Firebase.auth.currentUser ?: return onResult(null)
    val storageRef = Firebase.storage.reference.child("profile_images/${user.uid}.jpg")
    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { url ->
                Firebase.firestore.collection("users").document(user.uid)
                    .update("profileImage", url.toString())
                onResult(url.toString())
            }
        }
        .addOnFailureListener { onResult(null) }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (String) -> Unit,
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordError: String?,
    setPasswordError: (String?) -> Unit
) {
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = if (currentPasswordVisible) "Hide password" else "Show password")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF284545),
                        unfocusedBorderColor = Color(0xFFE1E8ED),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF284545),
                        unfocusedTextColor = Color(0xFF284545)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        onNewPasswordChange(it)
                        if (it.length < 6) setPasswordError("New password must be at least 6 characters")
                        else setPasswordError(null)
                    },
                    label = { Text("New Password") },
                    singleLine = true,
                    isError = passwordError != null,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = if (newPasswordVisible) "Hide password" else "Show password")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF284545),
                        unfocusedBorderColor = Color(0xFFE1E8ED),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF284545),
                        unfocusedTextColor = Color(0xFF284545)
                    )
                )
                if (passwordError != null) {
                    Text(text = passwordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        onConfirmPasswordChange(it)
                        if (it != newPassword) setPasswordError("Passwords do not match")
                        else setPasswordError(null)
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    isError = passwordError != null,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF284545),
                        unfocusedBorderColor = Color(0xFFE1E8ED),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF284545),
                        unfocusedTextColor = Color(0xFF284545)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passwordError == null
                        && currentPassword.isNotEmpty()
                        && newPassword.isNotEmpty()
                        && confirmPassword.isNotEmpty()
                        && newPassword == confirmPassword
                    ) {
                        onChangePassword(newPassword)
                    } else if (currentPassword.isEmpty()) {
                        setPasswordError("Current password is required")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545))
            ) {
                Text("Change Password", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable (() -> Unit))? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF284545),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .then(if (readOnly && onClick != null) Modifier.clickable { onClick() } else Modifier),
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF284545),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = trailingIcon,
            visualTransformation = if (isPassword && !passwordVisible) VisualTransformation.None else VisualTransformation.None, // Changed this line
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF284545),
                unfocusedBorderColor = Color(0xFFE1E8ED),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF284545),
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = readOnly
        )
    }
}