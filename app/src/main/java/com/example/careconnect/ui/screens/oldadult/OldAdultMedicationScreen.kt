package com.example.careconnect.ui.screens.oldadult

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.careconnect.ui.components.bottomnav.OldAdultBottomBar
import com.example.careconnect.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


data class MedicationModel(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val description: String = "",
    val patientId: String = "",
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val intervalHours: Int = 0,
    val timesPerDay: Int = 1,
    val firstHour: String = "08:00",
    val takenList: Map<String, Map<String, Boolean>> = emptyMap()
)


data class MedicationDose(
    val medication: MedicationModel,
    val doseTime: Date,
    val doseIndex: Int,
    var isTaken: Boolean
)


class MedicationViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _medications = MutableStateFlow<List<MedicationModel>>(emptyList())
    val medications = _medications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var userId: String? = null
    private var userName: String? = "Someone"
    private val sentAlerts = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            loadUserIdAndFetchMedications()
        }
    }


    private suspend fun loadUserIdAndFetchMedications() {
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
                    val userDoc = userQuery.documents.first()
                    userId = userDoc.getString("uid")
                    userName = userDoc.getString("name") ?: "Someone"
                    fetchMedications()
                } else {
                    Log.e("MedicationViewModel", "User document not found for email: ${currentUser.email}")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("MedicationViewModel", "Error loading user ID", e)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }


    private fun fetchMedications() {
        if (userId == null) return

        db.collection("medications")
            .whereEqualTo("patient_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MedicationViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val medicationList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            MedicationModel(
                                id = doc.id,
                                name = data["name"] as? String ?: "",
                                dosage = data["dosage"] as? String ?: "",
                                description = data["description"] as? String ?: "",
                                patientId = data["patient_id"] as? String ?: "",
                                startDate = (data["start_date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                endDate = (data["end_date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                intervalHours = (data["interval_hours"] as? Long)?.toInt() ?: 0,
                                timesPerDay = (data["times_per_day"] as? Long)?.toInt() ?: 1,
                                firstHour = data["first_hour"] as? String ?: "08:00",
                                takenList = data["taken_list"] as? Map<String, Map<String, Boolean>> ?: emptyMap()
                            )
                        } else {
                            null
                        }
                    }
                    _medications.value = medicationList
                }
                _isLoading.value = false
            }
    }


    fun markAsTaken(medicationId: String, doseDate: Date, doseIndex: Int) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("medications").document(medicationId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val existingTakenList = snapshot.get("taken_list") as? MutableMap<String, Any> ?: mutableMapOf()

                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(doseDate)
                    val dayMap = (existingTakenList[dateKey] as? MutableMap<String, Any>) ?: mutableMapOf()

                    dayMap[doseIndex.toString()] = true
                    existingTakenList[dateKey] = dayMap

                    transaction.update(docRef, "taken_list", existingTakenList)
                    null
                }.await()
            } catch (e: Exception) {
                Log.e("MedicationViewModel", "Error marking medication as taken", e)
            }
        }
    }

    fun checkAndSendMissedMedicationAlerts() {
        if (userId == null) return

        viewModelScope.launch {
            val now = Calendar.getInstance()
            val todayDoses = mutableListOf<MedicationDose>()
            val todayCal = Calendar.getInstance()

            _medications.value.forEach { med ->
                val startCal = Calendar.getInstance().apply { time = med.startDate }
                val endCal = Calendar.getInstance().apply { time = med.endDate }

                if (!todayCal.before(startCal) && !todayCal.after(endCal)) {
                    val firstHourParts = med.firstHour.split(":")
                    val hour = firstHourParts.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = firstHourParts.getOrNull(1)?.toIntOrNull() ?: 0

                    val firstTakeOfDay = (todayCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }

                    for (i in 0 until med.timesPerDay) {
                        val doseTimeCal = (firstTakeOfDay.clone() as Calendar).apply {
                            add(Calendar.HOUR, i * med.intervalHours)
                        }

                        if (doseTimeCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                            doseTimeCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)) {
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(doseTimeCal.time)
                            val isTaken = med.takenList[dateKey]?.get(i.toString()) == true
                            todayDoses.add(MedicationDose(med, doseTimeCal.time, i, isTaken))
                        }
                    }
                }
            }

            val missedDoses = todayDoses.filter { dose ->
                val doseTimeCal = Calendar.getInstance().apply { time = dose.doseTime }
                val alertWindowEnd = (doseTimeCal.clone() as Calendar).apply { add(Calendar.HOUR, 2) }
                val uniqueId = "${dose.medication.id}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dose.doseTime)}_${dose.doseIndex}"

                now.after(alertWindowEnd) && !dose.isTaken && !sentAlerts.contains(uniqueId)
            }

            if (missedDoses.isEmpty()) return@launch

            try {
                val relationsSnapshot = db.collection("caregiver_patients")
                    .whereEqualTo("patient_id", userId)
                    .get().await()
                val caregiverIds = relationsSnapshot.documents.mapNotNull { it.getString("caregiver_id") }

                if (caregiverIds.isEmpty()) return@launch

                val batch = db.batch()
                for (dose in missedDoses) {
                    for (caregiverId in caregiverIds) {
                        val notificationRef = db.collection("notifications").document()
                        val notificationData = hashMapOf(
                            "patient_id" to userId,
                            "patient_name" to userName,
                            "caregiver_id" to caregiverId,
                            "message" to "$userName missed medication: ${dose.medication.name} at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(dose.doseTime)}",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "type" to "missed_medication",
                            "isRead" to false
                        )
                        batch.set(notificationRef, notificationData)
                    }
                    val uniqueId = "${dose.medication.id}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dose.doseTime)}_${dose.doseIndex}"
                    sentAlerts.add(uniqueId)
                }
                batch.commit().await()
                Log.d("MedicationViewModel", "Sent alerts for ${missedDoses.size} missed medications.")
            } catch (e: Exception) {
                Log.e("MedicationViewModel", "Error sending missed medication alerts", e)
            }
        }
    }
}



@Composable
fun OldAdultMedicationScreen(
    navController: NavHostController,
    viewModel: MedicationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("medication") }
    val calendar = Calendar.getInstance()
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val medications by viewModel.medications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(medications) {
        if (medications.isNotEmpty()) {
            viewModel.checkAndSendMissedMedicationAlerts()
        }
    }


    val medicationsByDay = remember(medications, selectedYear, selectedMonth) {
        val dosesByDay = mutableMapOf<Int, MutableList<MedicationDose>>()
        val selectedCal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1)
        }
        val daysInMonth = selectedCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val currentDayCal = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, day)
            }
            medications.forEach { med ->
                val startCal = Calendar.getInstance().apply { time = med.startDate }
                val endCal = Calendar.getInstance().apply { time = med.endDate }

                if (!currentDayCal.before(startCal) && !currentDayCal.after(endCal)) {
                    val firstHourParts = med.firstHour.split(":")
                    val hour = firstHourParts.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = firstHourParts.getOrNull(1)?.toIntOrNull() ?: 0

                    val firstTakeOfDay = Calendar.getInstance().apply {
                        time = currentDayCal.time
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }

                    for (i in 0 until med.timesPerDay) {
                        val doseTimeCal = (firstTakeOfDay.clone() as Calendar).apply {
                            add(Calendar.HOUR, i * med.intervalHours)
                        }


                        if (doseTimeCal.get(Calendar.DAY_OF_MONTH) == day) {
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(doseTimeCal.time)
                            val isTaken = med.takenList[dateKey]?.get(i.toString()) == true

                            val dose = MedicationDose(
                                medication = med,
                                doseTime = doseTimeCal.time,
                                doseIndex = i,
                                isTaken = isTaken
                            )
                            dosesByDay.getOrPut(day) { mutableListOf() }.add(dose)
                        }
                    }
                }
            }
            dosesByDay[day]?.sortBy { it.doseTime }
        }
        dosesByDay
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            OldAdultBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    when (item) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // <-- Make screen scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Medication",
                fontSize = 22.sp,
                color = Color(0xFF284545)
            )
            Text(
                text = "Track your medications for each day",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            HorizontalCalendar(
                selectedDay = selectedDay,
                onDaySelected = { day -> selectedDay = day },
                currentDisplayCalendar = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Medications for day $selectedDay",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF284545)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                val medicationsForSelectedDay = medicationsByDay[selectedDay] ?: emptyList()
                val todayCalendar = Calendar.getInstance()
                val isToday = selectedYear == todayCalendar.get(Calendar.YEAR) &&
                        selectedMonth == todayCalendar.get(Calendar.MONTH) &&
                        selectedDay == todayCalendar.get(Calendar.DAY_OF_MONTH)


                if (medicationsForSelectedDay.isEmpty()) {
                    Text(
                        text = "No medications scheduled for this day.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        medicationsForSelectedDay.forEach { dose ->
                            MedicationItem(
                                medicationName = dose.medication.name,
                                quantity = dose.medication.dosage,
                                hourOfDay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dose.doseTime),
                                taken = dose.isTaken,
                                onMarkTaken = {
                                    viewModel.markAsTaken(dose.medication.id, dose.doseTime, dose.doseIndex)
                                },
                                isButtonEnabled = !dose.isTaken && isToday
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HorizontalCalendar(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    currentDisplayCalendar: Calendar
) {
    val scrollState = rememberScrollState()
    val dayWidthDp = 60.dp
    val dayWidthPx = with(LocalDensity.current) { dayWidthDp.toPx() }

    val monthCalendar = currentDisplayCalendar.clone() as Calendar
    monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    LaunchedEffect(selectedDay) {
        scrollState.animateScrollTo(((selectedDay - 1).coerceAtLeast(0) * dayWidthPx).toInt())
    }

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (day in 1..daysInMonth) {
            val isSelected = day == selectedDay
            val dayOfWeekName = getDayOfWeekName(day, monthCalendar)
            DayItem(day, dayOfWeekName, isSelected, onClick = { onDaySelected(day) })
        }
    }
}

@Composable
fun DayItem(day: Int, dayOfWeekName: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF284545) else Color(0xFFF7F7F7)
    val textColor = if (isSelected) Color.White else Color.Black

    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayOfWeekName,
            fontSize = 14.sp,
            color = textColor
        )
        Text(
            text = day.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun MedicationItem(
    medicationName: String,
    quantity: String,
    hourOfDay: String,
    taken: Boolean,
    onMarkTaken: () -> Unit,
    isButtonEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (taken) Color(0xFF284545) else Color(0xFFF7F7F7),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = medicationName,
            color = if (taken) Color.White else Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scheduled for: $hourOfDay",
            color = if (taken) Color.White else Color.DarkGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = quantity,
            color = if (taken) Color.White else Color.DarkGray,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (taken) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Taken",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(
                    onClick = onMarkTaken,
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF284545),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Mark as taken", fontSize = 14.sp)
                }
            }
        }
    }
}

fun getDayOfWeekName(dayOfMonth: Int, calendarContext: Calendar): String {
    val tempCalendar = calendarContext.clone() as Calendar
    tempCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    return when (tempCalendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> ""
    }
}
