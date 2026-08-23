package com.kmjs.virtualcamera.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.LogCategory
import com.kmjs.virtualcamera.core.LogEntry
import com.kmjs.virtualcamera.ui.theme.DarkConsoleBackground
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

@Composable
fun DiagnosticsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by DiagnosticsLogger.logsFlow.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<LogCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedCategory, searchQuery) {
        logs.filter { entry ->
            val matchCat = selectedCategory == null || entry.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.process.contains(searchQuery, ignoreCase = true) ||
                    entry.category.tag.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header and Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KMJS DIAGNOSTICS & TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Runtime Logs (${filteredLogs.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val exported = DiagnosticsLogger.exportAllLogs()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("KMJS Logs", exported))
                        Toast.makeText(context, "Copied ${logs.size} logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_copy_logs"),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VibrantPurple)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY", color = VibrantPurple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        DiagnosticsLogger.clear()
                    },
                    modifier = Modifier.testTag("btn_clear_logs"),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPurplePill),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, tint = VibrantLatencyAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLEAR", color = VibrantLatencyAlert, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter logs by message, process, or tag...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth().testTag("search_logs_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VibrantPurple,
                unfocusedBorderColor = VibrantCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("ALL") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VibrantPurplePill,
                    selectedLabelColor = VibrantPurpleDark,
                    containerColor = Color.White,
                    labelColor = TextSecondary
                ),
                shape = RoundedCornerShape(50)
            )

            LogCategory.values().forEach { cat ->
                val selected = selectedCategory == cat
                FilterChip(
                    selected = selected,
                    onClick = { selectedCategory = if (selected) null else cat },
                    label = { Text(cat.tag) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantPurplePill,
                        selectedLabelColor = VibrantPurpleDark,
                        containerColor = Color.White,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(50)
                )
            }
        }

        // Log Items List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLogs, key = { it.id }) { logEntry ->
                LogItemRow(logEntry = logEntry)
            }
        }
    }
}

@Composable
fun LogItemRow(logEntry: LogEntry) {
    val catColor = getCategoryColor(logEntry.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${logEntry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (logEntry.category == LogCategory.ERROR) VibrantLatencyAlert.copy(alpha = 0.5f) else VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = catColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = logEntry.category.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = catColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = logEntry.process,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = logEntry.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = logEntry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (logEntry.category == LogCategory.ERROR) VibrantLatencyAlert else TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            if (logEntry.throwableStackTrace != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkConsoleBackground
                ) {
                    Text(
                        text = logEntry.throwableStackTrace,
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantLatencyAlert.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

fun getCategoryColor(category: LogCategory): Color {
    return when (category) {
        LogCategory.MODULE -> VibrantPurple
        LogCategory.PROCESS -> Color(0xFF00639B)
        LogCategory.TARGET -> Color(0xFF1D5FA8)
        LogCategory.CAMERA -> Color(0xFF006874)
        LogCategory.INJECT -> VibrantSuccess
        LogCategory.RTSP -> VibrantWarning
        LogCategory.DECODER -> Color(0xFF5E5691)
        LogCategory.FRAME -> Color(0xFF0277BD)
        LogCategory.ERROR -> VibrantLatencyAlert
    }
}

