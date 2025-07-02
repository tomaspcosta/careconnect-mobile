package com.example.careconnect.ui.screens.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.careconnect.R

@Composable
fun LandingPageScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Welcome to",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF284545)
            )
            Text(
                text = "CareConnect",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF284545),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Your innovative solution for\n" +
                        "efficient home care management",
                fontSize = 20.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.careconnect_logo),
                contentDescription = "CareConnect Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconWithLabel(Icons.Filled.MedicalServices, "Medication\nReminders")
                    IconWithLabel(Icons.Filled.LocalDrink, "Hydration\nReminders")
                    IconWithLabel(Icons.Filled.Restaurant, "Nutrition\nReminders")
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        Button(
            onClick = onGetStartedClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF284545))
        ) {
            Text("Get Started", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun IconWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fullLabel: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = fullLabel,
            tint = Color(0xFF284545),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = fullLabel,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.DarkGray,
            lineHeight = 14.sp
        )
    }
}
