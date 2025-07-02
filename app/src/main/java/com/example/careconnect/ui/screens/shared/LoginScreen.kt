package com.example.careconnect.ui.screens.shared

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.careconnect.R
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale


@Composable
fun LoginScreen(
    navController: NavController,
    onNavigateToRegister: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onSimulateAdmin: () -> Unit = {},
    onSimulateOldAdult: () -> Unit = {},
    onSimulateFamily: () -> Unit = {},
    onSimulateCaregiver: () -> Unit = {},
    onNavigateToRole: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var emailReset by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun getUserRole(uid: String): String? {
        val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        return doc.getString("role")
    }

    fun updateLastLogin(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("last_login" to com.google.firebase.Timestamp.now()), SetOptions.merge())
    }

    fun login() {
        isLoading = true
        val auth = FirebaseAuth.getInstance()

        scope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                val uid = result.user?.uid ?: throw Exception("User ID not found")
                updateLastLogin(uid)

                val role = getUserRole(uid)
                if (role != null) {
                    when (role.lowercase(Locale.getDefault())) {
                        "admin" -> navController.navigate(Routes.ADMIN)
                        "caregiver" -> navController.navigate(Routes.CAREGIVER)
                        "family" -> navController.navigate(Routes.FAMILY)
                        "older_adult" -> navController.navigate(Routes.OLD_ADULT)
                        else -> Toast.makeText(context, "Unknown role", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: FirebaseAuthException) {
                val message = when (e.errorCode) {
                    "invalid-email" -> "The email address is not valid."
                    "user-disabled" -> "This user account has been disabled."
                    "user-not-found" -> "No user found for this email."
                    "wrong-password", "invalid-credential" -> "Incorrect email or password."
                    else -> "Login failed. Please try again."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Login failed. Please try again.", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_careconnect_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .height(350.dp)
                    .width(350.dp)
            )

            Spacer(modifier = Modifier.height(50.dp))


            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Enter your email", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Enter your password", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = TextStyle(fontSize = 14.sp),
                trailingIcon = {
                    val icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            ClickableText(
                text = buildAnnotatedString {
                    append("Forgot your password? ")
                    withStyle(SpanStyle(color = Color(0xFF1E88E5))) {
                        append("Click here to reset it.")
                    }
                },
                style = TextStyle(fontSize = 13.sp, textAlign = TextAlign.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                onClick = {
                    showResetPasswordDialog = true
                }
            )

            Button(
                onClick = { login() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            ClickableText(
                text = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(SpanStyle(color = Color(0xFF1E88E5))) {
                        append("Register here.")
                    }
                },
                style = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 32.dp),
                onClick = { onNavigateToRegister() }
            )
        }

        if (showResetPasswordDialog) {
            var resetError by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showResetPasswordDialog = false },
                title = { Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Enter your email to receive password reset instructions.")
                        OutlinedTextField(
                            value = emailReset,
                            onValueChange = { emailReset = it; resetError = "" },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = resetError.isNotEmpty()
                        )
                        if (resetError.isNotEmpty()) {
                            Text(resetError, color = Color.Red, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emailReset.isBlank()) {
                                resetError = "Please enter your email."
                                return@Button
                            }
                            isLoading = true
                            FirebaseAuth.getInstance().sendPasswordResetEmail(emailReset.trim())
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_LONG).show()
                                        showResetPasswordDialog = false
                                        emailReset = ""
                                    } else {
                                        resetError = task.exception?.localizedMessage ?: "Failed to send reset email."
                                    }
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Send", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetPasswordDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}


@Composable
fun UserTypeButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF284545),
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}
