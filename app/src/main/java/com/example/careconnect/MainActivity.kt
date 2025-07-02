package com.example.careconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.careconnect.ui.navigation.AppNavigation
import com.example.careconnect.ui.theme.CareconnectTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            CareconnectTheme(darkTheme = false) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
