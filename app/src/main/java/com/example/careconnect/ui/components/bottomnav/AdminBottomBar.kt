package com.example.careconnect.ui.components.bottomnav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person // Import the Person icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AdminBottomBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp, // Drop shadow effect
    ) {
        NavigationBar(containerColor = Color.White) {
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
                    indicatorColor = Color.Transparent,
                )
            )
            NavigationBarItem(
                selected = selectedItem == "users",
                onClick = { onItemSelected("users") },
                icon = { Icon(Icons.Filled.People, contentDescription = "Users") },
                label = { Text("Users") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF284545),
                    unselectedIconColor = Color.LightGray,
                    selectedTextColor = Color(0xFF284545),
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = Color.Transparent
                )
            )
            // Profile button added here
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
}