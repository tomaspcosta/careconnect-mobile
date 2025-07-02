package com.example.careconnect.ui.screens.shared

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No activity found in context chain")
}

@Composable
fun RegisterScreen(role: String, navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var birthdateError by remember { mutableStateOf<String?>(null) }
    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var repeatPasswordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text("Create account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF284545))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Please enter your details", fontSize = 16.sp, color = Color(0xFF284545))
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                CustomTextField(
                    value = fullName,
                    onValueChange = { fullName = it; fullNameError = null },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    isError = fullNameError != null,
                    errorMessage = fullNameError
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                CustomTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = "Email",
                    icon = Icons.Default.Email,
                    isError = emailError != null,
                    errorMessage = emailError
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Text(
                    "Birthdate",
                    fontSize = 14.sp,
                    color = Color(0xFF284545),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    textAlign = TextAlign.Start
                )

                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                birthdate = "%02d/%02d/%04d".format(day, month + 1, year)
                                birthdateError = null
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = if (birthdateError != null) {
                        ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(MaterialTheme.colorScheme.error))
                    } else null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF284545)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Birthdate", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (birthdate.isNotBlank()) birthdate else "")
                    }
                }

                if (birthdateError != null) {
                    Text(
                        text = birthdateError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                CustomTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; phoneNumberError = null },
                    label = "Phone Number",
                    icon = Icons.Default.Phone,
                    isError = phoneNumberError != null,
                    errorMessage = phoneNumberError
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                CustomTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    isError = passwordError != null,
                    errorMessage = passwordError
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                CustomTextField(
                    value = repeatPassword,
                    onValueChange = { repeatPassword = it; repeatPasswordError = null },
                    label = "Repeat Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    isError = repeatPasswordError != null,
                    errorMessage = repeatPasswordError
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Button(
                    onClick = {
                        fullNameError = null
                        emailError = null
                        birthdateError = null
                        phoneNumberError = null
                        passwordError = null
                        repeatPasswordError = null

                        var hasError = false
                        val nameRegex = Regex("^[A-Za-zÀ-ÿ\\s'-]+\$")
                        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,6}$")
                        val phoneRegex = Regex("^\\+?[0-9\\s]{7,15}$")

                        if (fullName.isBlank()) { fullNameError = "Full Name cannot be empty"; hasError = true }
                        else if (!nameRegex.matches(fullName)) { fullNameError = "Name must not contain numbers or symbols"; hasError = true }

                        if (email.isBlank()) { emailError = "Email cannot be empty"; hasError = true }
                        else if (!emailRegex.matches(email)) { emailError = "Invalid email format"; hasError = true }

                        if (birthdate.isBlank()) { birthdateError = "Birthdate cannot be empty"; hasError = true }

                        if (phoneNumber.isBlank()) { phoneNumberError = "Phone Number cannot be empty"; hasError = true }
                        else if (!phoneRegex.matches(phoneNumber)) { phoneNumberError = "Invalid phone number format"; hasError = true }

                        if (password.isBlank()) { passwordError = "Password cannot be empty"; hasError = true }
                        else if (password.length < 3) { passwordError = "Password must be at least 3 characters"; hasError = true }
                        else if (!password.any { it.isUpperCase() }) { passwordError = "Password must contain uppercase letter"; hasError = true }
                        else if (!password.any { it.isDigit() }) { passwordError = "Password must contain a number"; hasError = true }

                        if (repeatPassword.isBlank()) { repeatPasswordError = "Repeat Password cannot be empty"; hasError = true }
                        else if (password != repeatPassword) { repeatPasswordError = "Passwords do not match"; hasError = true }

                        if (hasError) {
                            scope.launch { snackbarHostState.showSnackbar("Please correct the errors above.") }
                            return@Button
                        }

                        val auth = FirebaseAuth.getInstance()
                        val firestore = FirebaseFirestore.getInstance()

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid ?: return@addOnSuccessListener
                                val userData = hashMapOf(
                                    "uid" to uid,
                                    "email" to email,
                                    "role" to role,
                                    "name" to fullName,
                                    "dob" to birthdate,
                                    "phone" to phoneNumber,
                                    "createdAt" to Timestamp.now()
                                )
                                firestore.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Registration successful")
                                            navController.popBackStack()
                                            navController.navigate("login")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        scope.launch { snackbarHostState.showSnackbar("Failed to save profile: ${e.message}") }
                                    }
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    val message = if (e is FirebaseAuthUserCollisionException) {
                                        "Email is already registered"
                                    } else e.message ?: "Registration failed"
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Arrow", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF284545), modifier = Modifier.padding(bottom = 6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { if (onClick == null) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() } else Modifier),
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF284545), modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (isPassword) {
                {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle Password Visibility")
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (onClick != null) KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.None) else KeyboardOptions.Default,
            readOnly = onClick != null || readOnly,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF284545),
                unfocusedBorderColor = Color(0xFFE1E8ED),
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF284545),
                unfocusedTextColor = Color(0xFF284545)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
