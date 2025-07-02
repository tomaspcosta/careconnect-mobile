package com.example.careconnect.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.example.careconnect.R
import com.example.careconnect.ui.screens.admin.AdminDashboardScreen
import com.example.careconnect.ui.screens.admin.UserManagementScreen
import com.example.careconnect.ui.screens.admin.AdminProfileScreen
import com.example.careconnect.ui.screens.caregiver.CaregiverDashboardScreen
import com.example.careconnect.ui.screens.caregiver.CaregiverPatientProfileScreen
import com.example.careconnect.ui.screens.caregiver.CaregiverPatientsScreen
import com.example.careconnect.ui.screens.caregiver.CaregiverProfileScreen
import com.example.careconnect.ui.screens.caregiver.CaregiverAlertsScreen
import com.example.careconnect.ui.screens.family.*
import com.example.careconnect.ui.screens.oldadult.*
import com.example.careconnect.ui.screens.shared.LandingPageScreen
import com.example.careconnect.ui.screens.shared.LoginScreen
import com.example.careconnect.ui.screens.shared.RegisterScreen
import com.example.careconnect.ui.screens.shared.RegisterUserTypeScreen

object Routes {
    const val LANDING = "landing"
    const val LOGIN = "login"
    const val REGISTER_USER_TYPE = "register_user_type"
    const val REGISTER_SCREEN = "register_screen"
    const val ADMIN = "admin"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_PROFILE = "admin_profile"
    const val CAREGIVER = "caregiver"
    const val FAMILY = "family"
    const val OLD_ADULT = "old_adult"
    const val OLD_ADULT_CHECKIN = "old_adult_checkin"
    const val OLD_ADULT_TASKS = "old_adult_tasks"
    const val OLD_ADULT_CAREGIVERS = "old_adult_caregivers"
    const val OLD_ADULT_HELP = "old_adult_help"
    const val OLD_ADULT_PROFILE = "old_adult_profile"
    const val OLD_ADULT_HYDRATION = "old_adult_hydration"
    const val OLD_ADULT_NUTRITION = "old_adult_nutrition"
    const val OLD_ADULT_EMERGENCY = "old_adult_emergency"
    const val OLD_ADULT_MEDICATION = "old_adult_medication"
    const val CAREGIVER_PATIENTS = "caregiver_patients"
    const val CAREGIVER_PROFILE = "caregiver_profile"
    const val FAMILY_PATIENTS = "family_patients"
    const val FAMILY_ALERTS = "family_alerts"
    const val FAMILY_PROFILE = "family_profile"
    const val CAREGIVER_PATIENT_PROFILE = "caregiver_patient_profile"
    const val FAMILY_OLD_ADULT = "family_old_adult"
    const val CAREGIVER_ALERTS = "caregiver_alerts"
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LANDING) {
        composable(Routes.LANDING) {
            LandingPageScreen(
                onGetStartedClick = { navController.navigate(Routes.LOGIN) },
                onLoginClick = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                onNavigateToRegister = { navController.navigate(Routes.REGISTER_USER_TYPE) },
                onForgotPasswordClick = { /* ação futura */ },
                onNavigateToRole = { role ->
                    when (role) {
                        "admin" -> navController.navigate(Routes.ADMIN)
                        "caregiver" -> navController.navigate(Routes.CAREGIVER)
                        "family" -> navController.navigate(Routes.FAMILY)
                        "older adult" -> navController.navigate(Routes.OLD_ADULT)
                        else -> {
                            Toast.makeText(
                                navController.context,
                                "Unknown role: $role",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
        composable(Routes.REGISTER_USER_TYPE) {
            RegisterUserTypeScreen(navController = navController)
        }
        composable(Routes.ADMIN) { AdminDashboardScreen(navController) }
        composable(Routes.ADMIN_PROFILE) { AdminProfileScreen(navController) }
        composable(Routes.CAREGIVER) { CaregiverDashboardScreen(navController) }
        composable(Routes.FAMILY) { FamilyDashboardScreen(navController) }

        composable(Routes.OLD_ADULT) { OldAdultDashboardScreen(navController) }
        composable(Routes.OLD_ADULT_CHECKIN) { OldAdultCheckInScreen(navController) }
        composable(Routes.OLD_ADULT_TASKS) { OldAdultTasksScreen(navController) }
        composable(Routes.OLD_ADULT_CAREGIVERS) { CaregiversScreen(navController) }
        composable(Routes.OLD_ADULT_HELP) { OldAdultHelpScreen(navController) }
        composable(Routes.OLD_ADULT_HYDRATION) { OldAdultHydrationScreen(navController) }
        composable(Routes.OLD_ADULT_NUTRITION) { OldAdultNutritionScreen(navController) }
        composable(Routes.OLD_ADULT_EMERGENCY) { OldAdultEmergencyScreen(navController) }
        composable(Routes.OLD_ADULT_MEDICATION) { OldAdultMedicationScreen(navController) }

        composable(Routes.ADMIN_USERS) { UserManagementScreen(navController) }
        composable("${Routes.REGISTER_SCREEN}/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: ""
            RegisterScreen(role = role, navController = navController)
        }
        composable(Routes.OLD_ADULT_PROFILE) { OldAdultProfileScreen(navController) }
        composable(Routes.CAREGIVER_PATIENTS) { CaregiverPatientsScreen(navController) }
        composable(Routes.CAREGIVER_PROFILE) { CaregiverProfileScreen(navController) }
        composable(Routes.FAMILY_PATIENTS) { FamilyPatientsScreen(navController) }
        composable(Routes.FAMILY_ALERTS) { FamilyAlertsScreen(navController) }
        composable(Routes.CAREGIVER_ALERTS) { CaregiverAlertsScreen(navController) }
        composable(
            route = Routes.CAREGIVER_PATIENT_PROFILE + "/{patientUid}",
            arguments = listOf(navArgument("patientUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientUid = backStackEntry.arguments?.getString("patientUid")
            CaregiverPatientProfileScreen(navController, patientUid)
        }
        composable(Routes.FAMILY_PROFILE) { FamilyProfileScreen(navController) }

        composable(
            route = Routes.FAMILY_OLD_ADULT + "/{patientUid}",
            arguments = listOf(navArgument("patientUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientUid = backStackEntry.arguments?.getString("patientUid")
            FamilyOldAdultScreen(navController, patientUid)
        }
    }
}
