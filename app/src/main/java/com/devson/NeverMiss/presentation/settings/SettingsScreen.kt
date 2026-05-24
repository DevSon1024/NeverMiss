package com.devson.NeverMiss.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Dialog States
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Backup Success Toasts
    LaunchedEffect(state.backupSuccess) {
        if (state.backupSuccess == true) {
            Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_LONG).show()
            viewModel.clearStatus()
        }
    }
    LaunchedEffect(state.restoreSuccess) {
        if (state.restoreSuccess == true) {
            Toast.makeText(context, "Data successfully restored!", Toast.LENGTH_LONG).show()
            viewModel.clearStatus()
        }
    }
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = ObsidianBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // THEME SECTION
                SettingsSectionTitle("APPEARANCE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Application Theme", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                UserPreferences.AppTheme.LIGHT to "Light",
                                UserPreferences.AppTheme.DARK to "Dark",
                                UserPreferences.AppTheme.SYSTEM to "System"
                            )
                            themes.forEach { (themeEnum, label) ->
                                val isSelected = state.theme == themeEnum
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBg)
                                        .clickable { viewModel.setTheme(themeEnum) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.White else TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(0.7f)) {
                                    Text("Dynamic Colors", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Use wallpaper palette colors (Android 12+)", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = state.dynamicColorsEnabled,
                                    onCheckedChange = viewModel::setDynamicColors
                                )
                            }
                        }
                    }
                }

                // REMINDERS CUSTOMIZATION
                SettingsSectionTitle("ALARM ENGINE CUSTOMIZATION")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom Snooze Duration", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Time to wait when clicking Snooze on a reminder", color = TextSecondaryDark, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val snoozeOptions = listOf(5, 10, 15, 30)
                            snoozeOptions.forEach { mins ->
                                val isSelected = state.snoozeDurationMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBg)
                                        .clickable { viewModel.setSnoozeDuration(mins) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${mins}m", color = if (isSelected) Color.White else TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Max Streak Freezes Limit", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Maximum freeze counts you are allowed to accumulate", color = TextSecondaryDark, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val freezeOptions = listOf(3, 5, 8, 10)
                            freezeOptions.forEach { limit ->
                                val isSelected = state.maxStreakFreezes == limit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else ObsidianBg)
                                        .clickable { viewModel.setMaxStreakFreezes(limit) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$limit Coins", color = if (isSelected) Color.White else TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // APP LOCK SECTION
                SettingsSectionTitle("SECURITY")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPinDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SecondaryTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Lock PIN", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                val statusStr = if (state.appLockPin.isEmpty()) "Disabled" else "Enabled (Tap to change/disable)"
                                Text(statusStr, color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
                    }
                }

                // PERMISSIONS SECTION
                SettingsSectionTitle("SYSTEM PERMISSIONS SHORTCUTS")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column {
                        PermissionShortcutItem(
                            title = "Notification Permissions",
                            subtitle = "Verify system banners settings",
                            icon = Icons.Default.Notifications,
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        )
                        Divider(color = BorderSlate, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        PermissionShortcutItem(
                            title = "Exact Alarms Permission",
                            subtitle = "Required for reliable exact alarms (Android 12+)",
                            icon = Icons.Default.AccessTime,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Exact alarm automatically granted on your Android version", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        Divider(color = BorderSlate, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        PermissionShortcutItem(
                            title = "Ignore Battery Optimizations",
                            subtitle = "Prevents OS from killing background alarms",
                            icon = Icons.Default.BatteryAlert,
                            onClick = {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // BACKUP & RESTORE SECTION
                SettingsSectionTitle("DATA IMPORT & EXPORT")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Exports data to cache file first, then loads it into clipboard
                                    val backupFile = File(context.cacheDir, "nevermiss_backup.json")
                                    viewModel.exportBackup(backupFile)
                                    
                                    // Simple check: we also copy the serialized text directly to clipboard
                                    // which is executed inside the ViewModel. Once done, let's copy to clipboard in VM
                                    // or handle here when state is updated.
                                    // To make it easy: let's launch code that reads the written backup file and copies to clipboard!
                                    scope.launch {
                                        // Wait 500ms for export completion
                                        delay(300)
                                        if (backupFile.exists()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("NeverMiss Backup", backupFile.readText())
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Export Backup JSON", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Serializes database and copies text to clipboard", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
                        }
                        Divider(color = BorderSlate, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showImportDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Import Backup JSON", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Paste text to restore all streaks and history", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            // DIALOGS

            // 1. PIN Lock dialog
            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false; enteredPin = ""; confirmPin = "" },
                    title = { Text("Configure PIN Lock", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Enter 4-digit PIN (leave empty to disable App Lock):", color = TextSecondaryDark, fontSize = 12.sp)
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = { if (it.length <= 4) enteredPin = it },
                                label = { Text("New PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                            if (enteredPin.isNotEmpty()) {
                                OutlinedTextField(
                                    value = confirmPin,
                                    onValueChange = { if (it.length <= 4) confirmPin = it },
                                    label = { Text("Confirm PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (enteredPin.isEmpty()) {
                                    viewModel.setAppLockPin("")
                                    Toast.makeText(context, "App lock PIN disabled", Toast.LENGTH_SHORT).show()
                                    showPinDialog = false
                                } else if (enteredPin.length == 4 && enteredPin == confirmPin) {
                                    viewModel.setAppLockPin(enteredPin)
                                    Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                                    showPinDialog = false
                                } else {
                                    Toast.makeText(context, "PINs do not match or are not 4 digits!", Toast.LENGTH_SHORT).show()
                                }
                                enteredPin = ""
                                confirmPin = ""
                            }
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinDialog = false; enteredPin = ""; confirmPin = "" }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                    },
                    containerColor = CardSlate
                )
            }

            // 2. Import JSON backup dialog
            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false; importText = "" },
                    title = { Text("Restore Backup JSON", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Paste the copied backup JSON text below. WARNING: This clears all existing streaks!", color = TextSecondaryDark, fontSize = 12.sp)
                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                placeholder = { Text("Paste JSON here...", color = TextSecondaryDark) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (importText.trim().isNotEmpty()) {
                                    viewModel.importBackup(importText)
                                    showImportDialog = false
                                    importText = ""
                                } else {
                                    Toast.makeText(context, "JSON cannot be empty!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Restore", fontWeight = FontWeight.Bold, color = AccentCoral)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false; importText = "" }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                    },
                    containerColor = CardSlate
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondaryDark,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun PermissionShortcutItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondaryDark)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = TextSecondaryDark, fontSize = 11.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
    }
}
