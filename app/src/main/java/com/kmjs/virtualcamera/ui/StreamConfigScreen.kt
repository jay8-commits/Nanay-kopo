package com.kmjs.virtualcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.core.DecoderStatus
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.core.StreamStatus
import com.kmjs.virtualcamera.core.VirtualCameraConfig
import com.kmjs.virtualcamera.runtime.KMJSModuleLoader
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
fun StreamConfigScreen(
    modifier: Modifier = Modifier
) {
    val systemState by StateRepository.systemState.collectAsStateWithLifecycle()
    val config by StateRepository.config.collectAsStateWithLifecycle()

    var rtspUrl by remember(config.rtspUrl) { mutableStateOf(config.rtspUrl) }
    var username by remember(config.username) { mutableStateOf(config.username) }
    var password by remember(config.password) { mutableStateOf(config.password) }
    var selectedWidth by remember(config.width) { mutableIntStateOf(config.width) }
    var selectedHeight by remember(config.height) { mutableIntStateOf(config.height) }
    var selectedFps by remember(config.fps) { mutableIntStateOf(config.fps) }
    var selectedRotation by remember(config.rotation) { mutableIntStateOf(config.rotation) }
    var selectedFormat by remember(config.pixelFormat) { mutableStateOf(config.pixelFormat) }
    var testPatternEnabled by remember(config.testPatternEnabled) { mutableStateOf(config.testPatternEnabled) }

    fun saveConfig() {
        val updated = config.copy(
            rtspUrl = rtspUrl,
            username = username,
            password = password,
            width = selectedWidth,
            height = selectedHeight,
            fps = selectedFps,
            rotation = selectedRotation,
            pixelFormat = selectedFormat,
            testPatternEnabled = testPatternEnabled
        )
        StateRepository.updateConfig(updated)
        KMJSModuleLoader.getDecoder().config = updated
        DiagnosticsLogger.rtsp("Stream configuration updated: ${updated.width}x${updated.height} @ ${updated.fps}fps, format=${updated.pixelFormat.displayName}")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "STREAM PIPELINE & DECODER",
                style = MaterialTheme.typography.labelSmall,
                color = VibrantPurple,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stream Configuration",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Stream Connection & Credentials Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stream_credentials_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Cast, contentDescription = null, tint = VibrantPurple)
                    Text("RTSP Stream Input", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = rtspUrl,
                    onValueChange = {
                        rtspUrl = it
                        saveConfig()
                    },
                    label = { Text("RTSP Stream URL") },
                    modifier = Modifier.fillMaxWidth().testTag("rtsp_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibrantPurple,
                        unfocusedBorderColor = VibrantCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            saveConfig()
                        },
                        label = { Text("Username (Optional)") },
                        modifier = Modifier.weight(1f).testTag("username_input"),
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
                        value = password,
                        onValueChange = {
                            password = it
                            saveConfig()
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.weight(1f).testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibrantPurple,
                            unfocusedBorderColor = VibrantCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Stream Buttons Action Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stream_action_buttons_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("STREAM & DECODER ACTIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            saveConfig()
                            DiagnosticsLogger.rtsp("[UI_ACTION] Connect button tapped for URL: $rtspUrl")
                            KMJSModuleLoader.getDecoder().connect(rtspUrl, username, password)
                        },
                        modifier = Modifier.weight(1f).testTag("btn_connect_rtsp"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CONNECT", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            DiagnosticsLogger.rtsp("[UI_ACTION] Disconnect button tapped")
                            KMJSModuleLoader.getDecoder().disconnect()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_disconnect_rtsp"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPurplePill),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = VibrantPurpleDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DISCONNECT", color = VibrantPurpleDark, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            saveConfig()
                            DiagnosticsLogger.decoder("[UI_ACTION] Start Decoder button tapped")
                            KMJSModuleLoader.getDecoder().start()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_start_decoder"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantSuccess),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START DECODER", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            DiagnosticsLogger.decoder("[UI_ACTION] Stop Decoder button tapped")
                            KMJSModuleLoader.getDecoder().stop()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_stop_decoder"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantLatencyAlert),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Output Format & Pipeline Options Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stream_format_options_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = VibrantPurple)
                    Text("Frame Delivery Parameters", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                // Resolution Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resolution", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val resolutions = listOf(
                            Pair(1920, 1080) to "1080p",
                            Pair(1280, 720) to "720p",
                            Pair(854, 480) to "480p",
                            Pair(640, 360) to "360p"
                        )
                        resolutions.forEach { (res, label) ->
                            val selected = selectedWidth == res.first && selectedHeight == res.second
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedWidth = res.first
                                    selectedHeight = res.second
                                    saveConfig()
                                },
                                label = { Text(label) },
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
                }

                // FPS Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Target Framerate (FPS)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { fps ->
                            val selected = selectedFps == fps
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedFps = fps
                                    saveConfig()
                                },
                                label = { Text("$fps FPS") },
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
                }

                // Rotation Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Orientation Rotation", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 90, 180, 270).forEach { rot ->
                            val selected = selectedRotation == rot
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedRotation = rot
                                    saveConfig()
                                },
                                label = { Text("$rot°") },
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
                }

                // Pixel Format Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pixel Format", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelFormat.values().forEach { format ->
                            val selected = selectedFormat == format
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedFormat = format
                                    saveConfig()
                                },
                                label = { Text(format.displayName) },
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
                }

                // Test Pattern Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pattern Test Frame Mode", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Generates SMPTE color bars with moving timecode without RTSP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Switch(
                        checked = testPatternEnabled,
                        onCheckedChange = {
                            testPatternEnabled = it
                            saveConfig()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VibrantPurple,
                            uncheckedTrackColor = VibrantCardBorder
                        )
                    )
                }
            }
        }

        // Live Stream Stats Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("stream_stats_display_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("STREAM & DECODER METRICS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stream Status:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        systemState.streamStatus.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (systemState.streamStatus == StreamStatus.CONNECTED) VibrantSuccess else VibrantLatencyAlert
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Decoder Status:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        systemState.decoderStatus.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (systemState.decoderStatus == DecoderStatus.RUNNING) VibrantSuccess else TextSecondary
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Delivered Framerate:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("%.1f FPS".format(systemState.currentFps), style = MaterialTheme.typography.bodyMedium, color = VibrantPurpleDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Received Frames:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("#${systemState.framesReceived}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontFamily = FontFamily.Monospace)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dropped Frames:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${systemState.droppedFrames}", style = MaterialTheme.typography.bodyMedium, color = if (systemState.droppedFrames > 0) VibrantWarning else VibrantSuccess, fontFamily = FontFamily.Monospace)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Latency:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${systemState.latencyMs} ms", style = MaterialTheme.typography.bodyMedium, color = VibrantLatencyAlert, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

