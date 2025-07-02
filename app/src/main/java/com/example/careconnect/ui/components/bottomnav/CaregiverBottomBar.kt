package com.example.careconnect.ui.components.bottomnav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications // Import for Notifications icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CaregiverBottomBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            selected = selectedItem == "dashboard",
            onClick = { onItemSelected("dashboard") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF284545),
                unselectedIconColor = Color.LightGray,
                selectedTextColor = Color(0xFF284545),
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedItem == "patients",
            onClick = { onItemSelected("patients") },
            icon = { Icon(Icons.Filled.Group, contentDescription = "Patients") },
            label = { Text("Patients") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF284545),
                unselectedIconColor = Color.LightGray,
                selectedTextColor = Color(0xFF284545),
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        // Alerts button added here
        NavigationBarItem(
            selected = selectedItem == "alerts",
            onClick = { onItemSelected("alerts") },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alerts") },
            label = { Text("Alerts") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF284545),
                unselectedIconColor = Color.LightGray,
                selectedTextColor = Color(0xFF284545),
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedItem == "profile",
            onClick = { onItemSelected("profile") },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF284545),
                unselectedIconColor = Color.LightGray,
                selectedTextColor = Color(0xFF284545),
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
    }
}