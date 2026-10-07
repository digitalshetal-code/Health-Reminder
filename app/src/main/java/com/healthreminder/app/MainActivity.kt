package com.healthreminder.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

val Context.dataStore by preferencesDataStore(name = "health_prefs")

data class Medicine(
    val id: Int,
    val name: String,
    val time: String,
    val taken: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthReminderApp()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "health_reminder",
                "Health Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Water and Medicine reminders" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthReminderApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💚 Health Reminder", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.WaterDrop, null) },
                    label = { Text("Water") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Medication, null) },
                    label = { Text("Medicine") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (selectedTab == 0) WaterScreen() else MedicineScreen()
        }
    }
}

@Composable
fun WaterScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var glasses by remember { mutableStateOf(0) }
    val goal = 8

    // Load saved data
    LaunchedEffect(Unit) {
        val saved = context.dataStore.data.map { it[intPreferencesKey("glasses")] ?: 0 }.first()
        glasses = saved
    }

    // Save data when changes
    fun saveGlasses(value: Int) {
        scope.launch {
            context.dataStore.edit { it[intPreferencesKey("glasses")] = value }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.WaterDrop, null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$glasses / $goal",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text("Glasses today", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { glasses.toFloat() / goal },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = Color(0xFF1976D2)
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (glasses > 0) {
                                    glasses--
                                    saveGlasses(glasses)
                                }
                            },
                            enabled = glasses > 0
                        ) { Text("− Remove") }
                        Button(
                            onClick = {
                                if (glasses < goal) {
                                    glasses++
                                    saveGlasses(glasses)
                                    if (glasses == goal) {
                                        sendNotification(
                                            context,
                                            "🎉 Goal Complete!",
                                            "Aapne aaj $goal glasses paani piya!"
                                        )
                                    }
                                }
                            },
                            enabled = glasses < goal
                        ) { Text("+ Add Glass") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("💡 Tip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Roz 8-10 glasses paani piyein. Subah uthte hi 1 glass zaroor piyein.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🔔 Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scheduleWaterReminder(context)
                            sendNotification(context, "✅ Reminder Set", "Har 2 ghante paani peene ka reminder aayega")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Set Water Reminder (Every 2 hrs)") }
                }
            }
        }
    }
}

@Composable
fun MedicineScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var medicines by remember { mutableStateOf(listOf<Medicine>()) }
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }

    // Load medicines
    LaunchedEffect(Unit) {
        val saved = context.dataStore.data.map { it[stringPreferencesKey("medicines")] ?: "" }.first()
        if (saved.isNotEmpty()) {
            medicines = saved.split("||").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 4) {
                    Medicine(parts[0].toIntOrNull() ?: 0, parts[1], parts[2], parts[3].toBoolean())
                } else null
            }
        } else {
            medicines = listOf(
                Medicine(1, "Vitamin D", "8:00 AM"),
                Medicine(2, "Blood Pressure", "2:00 PM"),
                Medicine(3, "Calcium", "9:00 PM")
            )
        }
    }

    fun saveMedicines(list: List<Medicine>) {
        scope.launch {
            val encoded = list.joinToString("||") { "${it.id}|${it.name}|${it.time}|${it.taken}" }
            context.dataStore.edit { it[stringPreferencesKey("medicines")] = encoded }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Medicine")
            }
        }

        items(medicines) { med ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = med.taken,
                        onCheckedChange = { checked ->
                            medicines = medicines.map {
                                if (it.id == med.id) it.copy(taken = checked) else it
                            }
                            saveMedicines(medicines)
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("⏰ ${med.time}", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = {
                        medicines = medicines.filter { it.id != med.id }
                        saveMedicines(medicines)
                    }) {
                        Icon(Icons.Filled.Delete, "Delete")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "✅ Total: ${medicines.count { it.taken }} / ${medicines.size} taken today",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Medicine") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Medicine name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("Time (e.g. 8:00 AM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank() && newTime.isNotBlank()) {
                        medicines = medicines + Medicine(
                            id = (medicines.maxOfOrNull { it.id } ?: 0) + 1,
                            name = newName,
                            time = newTime
                        )
                        saveMedicines(medicines)
                        newName = ""
                        newTime = ""
                        showDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

fun sendNotification(context: Context, title: String, message: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = android.app.Notification.Builder(context, "health_reminder")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setAutoCancel(true)
        .build()
    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun scheduleWaterReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WaterReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val interval: Long = 2 * 60 * 60 * 1000 // 2 hours
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + interval,
        interval,
        pendingIntent
    )
}

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        sendNotification(
            context,
            "💧 Paani Peene ka Time!",
            "Aapko paani peena yaad dilaya ja raha hai. Ek glass piyein!"
        )
    }
}
