package com.example.careconnect.ui.screens.oldadult

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


data class TaskModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val date: Date = Date(),
    val patientId: String = "",
    val status: String = "pending"
)


class TasksViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _tasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var userId: String? = null
    private var userName: String? = "Someone"
    private val sentAlerts = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            loadUserIdAndFetchTasks()
        }
    }

    private suspend fun loadUserIdAndFetchTasks() {
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
                    fetchTasks()
                } else {
                    Log.e("TasksViewModel", "User document not found")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error loading user ID", e)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    private fun fetchTasks() {
        if (userId == null) return

        db.collection("tasks")
            .whereEqualTo("patient_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TasksViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val taskList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        data?.let {
                            TaskModel(
                                id = doc.id,
                                name = it["name"] as? String ?: "",
                                description = it["description"] as? String ?: "",
                                date = (it["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                patientId = it["patient_id"] as? String ?: "",
                                status = it["status"] as? String ?: "pending"
                            )
                        }
                    }
                    _tasks.value = taskList
                }
                _isLoading.value = false
            }
    }

    fun checkAndSendMissedTaskAlerts() {
        if (userId == null) return

        viewModelScope.launch {
            val now = Date()
            val missedTasks = _tasks.value.filter {
                it.date.before(now) && it.status == "pending" && !sentAlerts.contains(it.id)
            }

            if (missedTasks.isEmpty()) return@launch

            try {
                val relationsSnapshot = db.collection("caregiver_patients")
                    .whereEqualTo("patient_id", userId)
                    .get().await()
                val caregiverIds = relationsSnapshot.documents.mapNotNull { it.getString("caregiver_id") }

                if (caregiverIds.isEmpty()) return@launch

                val batch = db.batch()
                for (task in missedTasks) {
                    for (caregiverId in caregiverIds) {
                        val notificationRef = db.collection("notifications").document()
                        val notificationData = hashMapOf(
                            "patient_id" to userId,
                            "patient_name" to userName,
                            "caregiver_id" to caregiverId,
                            "message" to "$userName missed a task: ${task.name}",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "type" to "missed_task",
                            "isRead" to false
                        )
                        batch.set(notificationRef, notificationData)
                    }
                    // Mark alert as sent for this session
                    sentAlerts.add(task.id)
                }
                batch.commit().await()
                Log.d("TasksViewModel", "Sent alerts for ${missedTasks.size} missed tasks.")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error sending missed task alerts", e)
            }
        }
    }

    fun addTask(name: String, description: String, date: Date) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val task = hashMapOf(
                    "name" to name,
                    "description" to description,
                    "date" to com.google.firebase.Timestamp(date),
                    "patient_id" to userId,
                    "status" to "pending"
                )
                db.collection("tasks").add(task).await()
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error adding task", e)
            }
        }
    }

    fun updateTaskStatus(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                db.collection("tasks").document(taskId)
                    .update("status", if (completed) "completed" else "pending").await()
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error updating task status", e)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                db.collection("tasks").document(taskId).delete().await()
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error deleting task", e)
            }
        }
    }
}

@Composable
fun OldAdultTasksScreen(
    navController: NavHostController,
    viewModel: TasksViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedItem by remember { mutableStateOf("tasks") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskModel?>(null) }
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty()) {
            viewModel.checkAndSendMissedTaskAlerts()
        }
    }

    val filteredTasks = remember(tasks, selectedDay, selectedMonth, selectedYear) {
        tasks.filter { task ->
            val taskCalendar = Calendar.getInstance().apply { time = task.date }
            taskCalendar.get(Calendar.YEAR) == selectedYear &&
                    taskCalendar.get(Calendar.MONTH) == selectedMonth &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == selectedDay
        }.sortedBy { it.date }
    }

    val todayCalendar = Calendar.getInstance()
    val isTodaySelected = selectedYear == todayCalendar.get(Calendar.YEAR) &&
            selectedMonth == todayCalendar.get(Calendar.MONTH) &&
            selectedDay == todayCalendar.get(Calendar.DAY_OF_MONTH)

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF284545)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "My Tasks",
                fontSize = 22.sp,
                color = Color(0xFF284545),
                fontWeight = FontWeight.Bold
            )
            Text(
                "Manage your tasks for each day",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            HorizontalCalendar(
                selectedDay = selectedDay,
                onDaySelected = { day -> selectedDay = day },
                currentDisplayCalendar = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredTasks.isEmpty()) {
                Text(
                    "No tasks scheduled for this day.",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskItem(
                            task = task,
                            onCheckedChange = { completed ->
                                viewModel.updateTaskStatus(task.id, completed)
                            },
                            onDelete = { taskToDelete = task },
                            isCheckable = isTodaySelected
                        )
                    }
                }
            }
        }
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${taskToDelete?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(taskToDelete!!.id)
                        taskToDelete = null
                        Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { name, description, date ->
                viewModel.addTask(name, description, date)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskItem(
    task: TaskModel,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    isCheckable: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.status == "completed") Color(0xFFE8F5E9) else Color(0xFFF7F7F7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.status == "completed",
                onCheckedChange = onCheckedChange,
                enabled = isCheckable && task.status != "completed",
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF284545),
                    uncheckedColor = Color.DarkGray,
                    disabledUncheckedColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(task.date),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = Color.Red)
            }
        }
    }
}


@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, String, Date) -> Unit
) {
    val context = LocalContext.current
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf<Date?>(null) }
    var dateTimeString by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            selectedDateTime = calendar.time
            dateTimeString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(selectedDateTime!!)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dateTimeString,
                        onValueChange = { },
                        label = { Text("Date & Time") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Filled.CalendarToday, "Select Date & Time") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank() && selectedDateTime != null) {
                        onAddTask(taskName.trim(), taskDescription.trim(), selectedDateTime!!)
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
