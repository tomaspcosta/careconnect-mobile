package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class DashboardViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _userName = MutableStateFlow("User")
    val userName = _userName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchUserName()
    }

    private fun fetchUserName() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser
            if (currentUser?.email != null) {
                try {
                    val userQuery = db.collection("users")
                        .whereEqualTo("email", currentUser.email)
                        .limit(1)
                        .get()
                        .await()

                    if (!userQuery.isEmpty) {
                        _userName.value = userQuery.documents.first().getString("name") ?: "User"
                    } else {
                        Log.e("DashboardViewModel", "User document not found")
                    }
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error fetching user name", e)
                }
            }
            _isLoading.value = false
        }
    }
}


@Composable
fun OldAdultDashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("dashboard") }

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
        DashboardContent(Modifier.padding(innerPadding), navController, viewModel)
    }
}

@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: DashboardViewModel
) {
    val userName by viewModel.userName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            GreetingSection(userName)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Check-In",
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = Color(0xFF284545),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_CHECKIN) }
                )
                DashboardCard(
                    title = "Medication",
                    icon = Icons.Default.Medication,
                    backgroundColor = Color(0xFF284545),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_MEDICATION) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Hydration",
                    icon = Icons.Default.InvertColors,
                    backgroundColor = Color(0xFF284545),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_HYDRATION) }
                )
                DashboardCard(
                    title = "Nutrition",
                    icon = Icons.Filled.Restaurant,
                    backgroundColor = Color(0xFF284545),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_NUTRITION) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Tasks",
                    icon = Icons.Filled.Assignment,
                    backgroundColor = Color(0xFF284545),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_TASKS) }
                )
                DashboardCard(
                    title = "Emergency",
                    icon = Icons.Default.Warning,
                    backgroundColor = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Routes.OLD_ADULT_EMERGENCY) }
                )
            }
        }
    }
}

@Composable
fun GreetingSection(userName: String) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        in 18..21 -> "Good evening,"
        else -> "Good night,"
    }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val formattedDate = dateFormat.format(calendar.time)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = greeting,
            fontSize = 20.sp,
            color = Color.Black
        )
        Text(
            text = "$userName!",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.Black
        )
        Text(
            text = formattedDate,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 16.sp, color = Color.White)
        }
    }
}
