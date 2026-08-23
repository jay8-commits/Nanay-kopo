package com.kmjs.virtualcamera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.runtime.SupportedTargetRegistry
import com.kmjs.virtualcamera.runtime.TargetDefinition
import com.kmjs.virtualcamera.ui.theme.TextMuted
import com.kmjs.virtualcamera.ui.theme.TextPrimary
import com.kmjs.virtualcamera.ui.theme.TextSecondary
import com.kmjs.virtualcamera.ui.theme.VibrantBackground
import com.kmjs.virtualcamera.ui.theme.VibrantCardBorder
import com.kmjs.virtualcamera.ui.theme.VibrantLatencyAlert
import com.kmjs.virtualcamera.ui.theme.VibrantPurple
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleDark
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleLight
import com.kmjs.virtualcamera.ui.theme.VibrantPurplePill
import com.kmjs.virtualcamera.ui.theme.VibrantSuccess
import com.kmjs.virtualcamera.ui.theme.VibrantWarning
import com.kmjs.virtualcamera.ui.theme.VibrantWarningContainer

@Composable
fun TargetManagementScreen(
    modifier: Modifier = Modifier
) {
    val targets by SupportedTargetRegistry.targetsListFlow.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TARGET APPLICATION REGISTRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supported Targets (${targets.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("add_target_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Target", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Architectural notice card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VibrantPurpleLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantPurpleDark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(
                        text = "Modular Target Architecture",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adding a package configures process detection and adapter routing. Ensure target package matches verified camera pipeline before enabling injection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(targets, key = { it.packageName }) { target ->
                TargetItemCard(
                    target = target,
                    onToggleEnabled = { enabled ->
                        SupportedTargetRegistry.setEnabled(target.packageName, enabled)
                        DiagnosticsLogger.target("Target ${target.packageName} enabled=$enabled")
                    },
                    onDelete = {
                        SupportedTargetRegistry.unregister(target.packageName)
                        DiagnosticsLogger.target("Target ${target.packageName} removed from registry")
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTargetDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newTarget ->
                SupportedTargetRegistry.register(newTarget)
                DiagnosticsLogger.target("Custom target added: ${newTarget.packageName} (Adapter: ${newTarget.adapterType})")
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TargetItemCard(
    target: TargetDefinition,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_item_${target.packageName}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (target.enabled) VibrantPurple.copy(alpha = 0.3f) else VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (target.enabled) VibrantPurplePill else VibrantCardBorder,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                tint = if (target.enabled) VibrantPurpleDark else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = target.packageName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (target.enabled) TextPrimary else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Adapter: ${target.adapterType} | Status: ${if (target.enabled) "Active" else "Disabled"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (target.enabled) VibrantSuccess else VibrantLatencyAlert
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = target.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VibrantPurple,
                            uncheckedTrackColor = VibrantCardBorder
                        )
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete target", tint = TextMuted)
                    }
                }
            }

            // Supported APIs chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                target.supportedApis.forEach { api ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VibrantPurplePill
                    ) {
                        Text(
                            text = api.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantPurpleDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (target.compatibilityNotes.isNotBlank()) {
                Text(
                    text = target.compatibilityNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AddTargetDialog(
    onDismiss: () -> Unit,
    onAdd: (TargetDefinition) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var adapterType by remember { mutableStateOf("Camera2Hook") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Register New Target Package", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it.trim() },
                    label = { Text("Package Name (e.g. com.example.camera)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_package_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPurple,
                        unfocusedBorderColor = VibrantCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adapterType,
                    onValueChange = { adapterType = it },
                    label = { Text("Adapter (Camera2Hook / CameraXHook / LegacyCameraHook)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPurple,
                        unfocusedBorderColor = VibrantCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Compatibility Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPurple,
                        unfocusedBorderColor = VibrantCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (packageName.isNotBlank()) {
                        val newDef = TargetDefinition(
                            packageName = packageName,
                            processNameFilter = packageName,
                            supportedApis = listOf(CameraApiType.CAMERA2),
                            adapterType = adapterType,
                            enabled = true,
                            compatibilityNotes = notes
                        )
                        onAdd(newDef)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                shape = RoundedCornerShape(50),
                enabled = packageName.isNotBlank()
            ) {
                Text("Register Target", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

