package com.example.careconnect.ui.screens.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.careconnect.R
import com.example.careconnect.ui.navigation.Routes
import androidx.compose.ui.layout.ContentScale // Import ContentScale for image scaling

@Composable
fun RegisterUserTypeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally // This centers the Column's content horizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // --- REVISED IMAGE PLACEMENT AND SIZING ---
        // Using a Box to ensure the image is perfectly centered within its allocated space
        Box(
            modifier = Modifier
                .fillMaxWidth() // Make the Box fill the available horizontal space
                .height(250.dp), // Give the Box a larger fixed height to make the image bigger
            contentAlignment = Alignment.Center // Center the content (the Image) within this Box
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_careconnect_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxSize() // Make the Image fill the Box, constrained by Box's dimensions
                    .padding(16.dp), // Add some internal padding to prevent the logo from touching the edges of its Box
                contentScale = ContentScale.Fit // Scale the image to fit within the bounds, preserving its aspect ratio
            )
        }
        // --- END REVISED IMAGE PLACEMENT AND SIZING ---


        Spacer(modifier = Modifier.weight(1f)) // This pushes the content below the image towards the center/bottom

        Text(
            text = "I am a",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF284545)
        )

        Spacer(modifier = Modifier.height(24.dp))

        UserTypeButton2("Caregiver", Icons.Filled.Favorite) {
            navController.navigate("${Routes.REGISTER_SCREEN}/caregiver")
        }
        Spacer(modifier = Modifier.height(24.dp))

        UserTypeButton2("Family Member", Icons.Filled.Groups) {
            navController.navigate("${Routes.REGISTER_SCREEN}/family")
        }
        Spacer(modifier = Modifier.height(24.dp))

        UserTypeButton2("Older Adult", Icons.Filled.DirectionsWalk) {
            navController.navigate("${Routes.REGISTER_SCREEN}/older_adult")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Removed the 3 dots (pagination indicators) as per previous request

        Spacer(modifier = Modifier.height(24.dp)) // Keep a spacer if needed for bottom padding
    }
}

@Composable
fun UserTypeButton2(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545))
    ) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
    }
}