package com.example.careconnect.ui.components.bottomnav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun OldAdultBottomBar(selectedItem: String, onItemSelected: (String) -> Unit) {
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
            selected = selectedItem == "caregivers",
            onClick = { onItemSelected("caregivers") },
            icon = { Icon(Icons.Filled.Group, contentDescription = "Caregivers") },
            label = { Text("Caregivers") },
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
        NavigationBarItem(
            selected = selectedItem == "help",
            onClick = { onItemSelected("help") },
            icon = { Icon(Icons.Filled.Help, contentDescription = "Help") },
            label = { Text("Help") },
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