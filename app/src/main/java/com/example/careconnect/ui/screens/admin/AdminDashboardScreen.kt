package com.example.careconnect.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.AdminBottomBar
import com.example.careconnect.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("dashboard") }

    // --- State for fetched data ---
    var totalUsers by remember { mutableStateOf(0) }
    var oldAdults by remember { mutableStateOf(0) }
    var caregivers by remember { mutableStateOf(0) }
    var familyMembers by remember { mutableStateOf(0) }
    var admins by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    // Updated to include admin percentage
    var userDistributionData by remember { mutableStateOf(UserDistribution(0f, 0f, 0f, 0f)) }


    // --- Data Fetching Logic ---
    val firestore = Firebase.firestore

    LaunchedEffect(Unit) {
        try {
            val usersSnapshot = firestore.collection("users").get().await()

            var fetchedOld = 0
            var fetchedCare = 0
            var fetchedFam = 0
            var fetchedAdmins = 0
            var fetchedOther = 0

            for (document in usersSnapshot.documents) {
                val roleRaw = document.getString("role")
                val role = roleRaw?.lowercase()?.trim() ?: ""

                when (role) {
                    "older_adult" -> fetchedOld++
                    "caregiver" -> fetchedCare++
                    "family" -> fetchedFam++
                    "admin" -> fetchedAdmins++
                    else -> fetchedOther++
                }
            }

            // Update mutable states
            totalUsers = usersSnapshot.size()
            oldAdults = fetchedOld
            caregivers = fetchedCare
            familyMembers = fetchedFam
            admins = fetchedAdmins

            // Calculate percentages for the chart, now including Admins
            val totalForChart = (fetchedOld + fetchedCare + fetchedFam + fetchedAdmins).toFloat()
            userDistributionData = if (totalForChart > 0) {
                UserDistribution(
                    oldAdultsPercent = fetchedOld / totalForChart,
                    caregiversPercent = fetchedCare / totalForChart,
                    familyMembersPercent = fetchedFam / totalForChart,
                    adminsPercent = fetchedAdmins / totalForChart
                )
            } else {
                UserDistribution(0f, 0f, 0f, 0f) // Avoid division by zero
            }

            isLoading = false

        } catch (e: Exception) {
            Log.e("AdminDashboardScreen", "Error fetching user stats: ${e.message}", e)
            isLoading = false
            // TODO: Handle error gracefully, e.g., show a Snackbar to the user
        }
    }


    Scaffold(
        bottomBar = {
            AdminBottomBar(
                selectedItem = selectedItem,
                onItemSelected = {
                    selectedItem = it
                    if (it == "dashboard") {
                        navController.navigate(Routes.ADMIN) {
                            launchSingleTop = true
                        }
                    } else if (it == "users") {
                        navController.navigate(Routes.ADMIN_USERS) {
                            launchSingleTop = true
                        }
                    } else if (it == "profile") {
                        navController.navigate(Routes.ADMIN_PROFILE) {
                            popUpTo(Routes.ADMIN) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            GreetingSection()
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Pass all percentages to the chart, including admin
                UserDistributionChart(
                    oldAdultsPercent = userDistributionData.oldAdultsPercent,
                    caregiversPercent = userDistributionData.caregiversPercent,
                    familyMembersPercent = userDistributionData.familyMembersPercent,
                    adminsPercent = userDistributionData.adminsPercent // Pass admin percentage
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                StatsSection(
                    oldAdults = oldAdults,
                    caregivers = caregivers,
                    familyMembers = familyMembers,
                    admins = admins,
                    totalUsers = totalUsers
                )
            }
        }
    }
}

// Updated data class to include admin percentage
data class UserDistribution(
    val oldAdultsPercent: Float,
    val caregiversPercent: Float,
    val familyMembersPercent: Float,
    val adminsPercent: Float // New field for admin percentage
)


@Composable
fun GreetingSection() {
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = greeting, fontSize = 20.sp, color = Color.Black)
        Text(text = "Admin!", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
        Text(text = formattedDate, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun UserDistributionChart(
    oldAdultsPercent: Float,
    caregiversPercent: Float,
    familyMembersPercent: Float,
    adminsPercent: Float // New parameter for admin percentage
) {
    val oldAdultsColor = Color(0xFF8A63E9) // Purple
    val caregiversColor = Color(0xFFFF9680) // Orange
    val familyMembersColor = Color(0xFF40CFC2) // Teal
    val adminsColor = Color(0xFFE91E63) // Deep Pink for Admins - distinct from other colors

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "User Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF284545)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 100f
                        var currentStartAngle = 90f

                        // Old Adults Slice
                        if (oldAdultsPercent > 0) { // Only draw if there's a slice
                            drawArc(
                                color = oldAdultsColor,
                                startAngle = currentStartAngle,
                                sweepAngle = 360 * oldAdultsPercent,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            currentStartAngle += 360 * oldAdultsPercent
                        }


                        // Caregivers Slice
                        if (caregiversPercent > 0) {
                            drawArc(
                                color = caregiversColor,
                                startAngle = currentStartAngle,
                                sweepAngle = 360 * caregiversPercent,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            currentStartAngle += 360 * caregiversPercent
                        }


                        // Family Members Slice
                        if (familyMembersPercent > 0) {
                            drawArc(
                                color = familyMembersColor,
                                startAngle = currentStartAngle,
                                sweepAngle = 360 * familyMembersPercent,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            currentStartAngle += 360 * familyMembersPercent
                        }

                        // Admins Slice - NEW
                        if (adminsPercent > 0) {
                            drawArc(
                                color = adminsColor,
                                startAngle = currentStartAngle,
                                sweepAngle = 360 * adminsPercent,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            // No need to update currentStartAngle further
                        }

                        // Inner circle to make it a donut chart
                        drawCircle(color = Color.White, radius = size.minDimension / 2 - strokeWidth / 2)
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    LegendItem(oldAdultsColor, "Old Adults", String.format("%.1f%%", oldAdultsPercent * 100))
                    Spacer(modifier = Modifier.height(8.dp))
                    LegendItem(caregiversColor, "Caregivers", String.format("%.1f%%", caregiversPercent * 100))
                    Spacer(modifier = Modifier.height(8.dp))
                    LegendItem(familyMembersColor, "Family", String.format("%.1f%%", familyMembersPercent * 100))
                    Spacer(modifier = Modifier.height(8.dp)) // Space for the new item
                    LegendItem(adminsColor, "Admins", String.format("%.1f%%", adminsPercent * 100)) // New legend item
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, percentage: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF284545))
        percentage?.let {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SmallStatCard(label: String, value: Int, icon: ImageVector, backgroundColor: Color, modifier: Modifier = Modifier) { // Added backgroundColor param
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor), // Use passed background color
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF284545), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF284545), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF284545), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatsSection(
    oldAdults: Int,
    caregivers: Int,
    familyMembers: Int,
    admins: Int,
    totalUsers: Int
) {
    // Define specific colors for each card here
    val oldAdultsCardColor = Color(0xFFE3F2FD) // Light Blue
    val caregiversCardColor = Color(0xFFFFFDE7) // Light Yellow
    val familyMembersCardColor = Color(0xFFFCE4EC) // Light Pink
    val adminsCardColor = Color(0xFFE8F5E9)    // Light Green

    // First row: Old Adults and Caregivers
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallStatCard("Old Adults", oldAdults, Icons.Default.Elderly, oldAdultsCardColor, Modifier.weight(1f)) // Pass color
        SmallStatCard("Caregivers", caregivers, Icons.Default.MedicalServices, caregiversCardColor, Modifier.weight(1f)) // Pass color
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Second row: Family Members and Admins
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallStatCard("Family Members", familyMembers, Icons.Default.FamilyRestroom, familyMembersCardColor, Modifier.weight(1f)) // Pass color
        SmallStatCard("Admins", admins, Icons.Default.AdminPanelSettings, adminsCardColor, Modifier.weight(1f)) // Pass color
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Total Users card remains at the bottom
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.People, contentDescription = "Total Users", tint = Color(0xFF284545), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Total Users", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF284545))
            Text(text = totalUsers.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color(0xFF284545))
        }
    }
}