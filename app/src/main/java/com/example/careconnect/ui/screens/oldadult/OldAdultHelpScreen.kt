package com.example.careconnect.ui.screens.oldadult

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar

@Composable
fun OldAdultHelpScreen(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("help") }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            OldAdultBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                    when(item) {
                        "dashboard" -> navController.navigate("old_adult") { launchSingleTop = true }
                        "caregivers" -> navController.navigate("old_adult_caregivers") { launchSingleTop = true }
                        "tasks" -> navController.navigate("old_adult_tasks") { launchSingleTop = true }
                        "help" -> navController.navigate("old_adult_help") { launchSingleTop = true }
                        "profile" -> navController.navigate("old_adult_profile") { launchSingleTop = true }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título e subtítulo
            Text(
                text = "Help",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF284545)
            )
            Text(
                text = "Finds Answers and get help\nusing the app",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Layout dos cartões (3 linhas x 2 colunas)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpCard(
                        icon = Icons.Filled.CheckCircle,
                        text = "Tap twice a day\nto confirm you're ok",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    HelpCard(
                        icon = Icons.Filled.Medication,
                        text = "Tap when you’ve\ntaken your medicine",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpCard(
                        icon = Icons.Filled.InvertColors,
                        text = "Confirm when\nyou drink water",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    HelpCard(
                        icon = Icons.Filled.Restaurant,
                        text = "Mark each meal\nyou’ve eaten",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpCard(
                        icon = Icons.Filled.Assignment,
                        text = "See and complete\nyour daily tasks",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    HelpCard(
                        icon = Icons.Filled.Warning,
                        text = "Use only in urgent\ncases to alert caregivers",
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HelpCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF284545),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                color = Color(0xFF284545),
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
