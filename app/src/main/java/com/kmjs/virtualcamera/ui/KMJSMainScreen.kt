package com.kmjs.virtualcamera.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.core.DecoderStatus
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.core.KMJSSystemState
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.core.StreamStatus
import com.kmjs.virtualcamera.core.VirtualCameraConfig
import com.kmjs.virtualcamera.runtime.KMJSModuleLoader
import com.kmjs.virtualcamera.testing.PatternTestFrameProvider
import com.kmjs.virtualcamera.ui.theme.DarkConsoleAccent
import com.kmjs.virtualcamera.ui.theme.DarkConsoleBackground
import com.kmjs.virtualcamera.ui.theme.DarkConsoleMuted
import com.kmjs.virtualcamera.ui.theme.DarkConsoleSurface
import com.kmjs.virtualcamera.ui.theme.DarkConsoleText
import com.kmjs.virtualcamera.ui.theme.TextMuted
import com.kmjs.virtualcamera.ui.theme.TextPrimary
import com.kmjs.virtualcamera.ui.theme.TextSecondary
import com.kmjs.virtualcamera.ui.theme.VibrantBackground
import com.kmjs.virtualcamera.ui.theme.VibrantCardBorder
import com.kmjs.virtualcamera.ui.theme.VibrantErrorContainer
import com.kmjs.virtualcamera.ui.theme.VibrantLatencyAlert
import com.kmjs.virtualcamera.ui.theme.VibrantPurple
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleBorder
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleDark
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleLight
import com.kmjs.virtualcamera.ui.theme.VibrantPurplePill
import com.kmjs.virtualcamera.ui.theme.VibrantSuccess
import kotlinx.coroutines.delay

@Composable
fun KMJSMainScreen(
    modifier: Modifier = Modifier,
    onNavigateToStream: () -> Unit = {},
    onNavigateToTests: () -> Unit = {}
) {
    val systemState by StateRepository.systemState.collectAsStateWithLifecycle()
    val config by StateRepository.config.collectAsStateWithLifecycle()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewFrameCounter by remember { mutableLongStateOf(0L) }

    // Live preview refresh loop
    LaunchedEffect(systemState.decoderStatus) {
        while (true) {
            if (systemState.decoderStatus == DecoderStatus.RUNNING || KMJSModuleLoader.isInitialized) {
                previewFrameCounter++
                val frame = PatternTestFrameProvider.generateFrame(
                    width = 480,
                    height = 270,
                    pixelFormat = config.pixelFormat,
                    rotation = config.rotation,
                    frameNumber = previewFrameCounter
                )
                previewBitmap = frame.bitmap
            }
            delay(40L) // ~25 FPS UI preview
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vibrant Hero Banner Card
        VibrantHeroCard(
            state = systemState,
            onStartStop = {
                if (systemState.isConnected) {
                    KMJSModuleLoader.stopModule()
                } else {
                    KMJSModuleLoader.startModule()
                }
            }
        )

        // Live Virtual Camera Video Preview
        LiveVideoPreviewCard(
            bitmap = previewBitmap,
            systemState = systemState,
            config = config
        )

        // Vibrant 2x2 Telemetry Grid
        VibrantTelemetryGrid(state = systemState, config = config)

        // System Controls & Quick Navigation
        VibrantActionControls(
            systemState = systemState,
            onNavigateToStream = onNavigateToStream,
            onNavigateToTests = onNavigateToTests
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun VibrantHeroCard(
    state: KMJSSystemState,
    onStartStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantPurpleLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top action row with icon and toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VibrantPurpleDark,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Button(
                    onClick = onStartStop,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isConnected) VibrantPurple else VibrantPurpleDark
                    ),
                    modifier = Modifier.testTag(if (state.isConnected) "stop_camera_button" else "start_camera_button")
                ) {
                    Text(
                        text = if (state.isConnected) "STOP SERVICE" else "START SERVICE",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Target Process Details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Target Process",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.detectedPackage,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = VibrantPurpleDark,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Bottom 3-column stats bar with separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VibrantPurpleBorder)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "API",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.cameraApi.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "HOOK",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.hookStatus.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SOURCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RTSP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = state.lastErrorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VibrantErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VibrantLatencyAlert.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = VibrantLatencyAlert, modifier = Modifier.size(18.dp))
                        Text(
                            text = state.lastErrorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantLatencyAlert
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveVideoPreviewCard(
    bitmap: Bitmap?,
    systemState: KMJSSystemState,
    config: VirtualCameraConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_video_preview_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = VibrantPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Virtual Frame Stream",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = VibrantPurplePill
                ) {
                    Text(
                        text = "${config.width}x${config.height} @ ${config.fps}fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Canvas monitor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkConsoleBackground)
                    .border(1.dp, VibrantCardBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Virtual Camera Output Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = DarkConsoleMuted, modifier = Modifier.size(32.dp))
                        Text(
                            text = "Stream Standby / Start Service to Render",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkConsoleMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VibrantTelemetryGrid(state: KMJSSystemState, config: VirtualCameraConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VibrantStatCard(
                modifier = Modifier.weight(1f),
                label = "FPS",
                value = "%.1f".format(state.currentFps.coerceAtLeast(0f)),
                subText = "avg",
                valueColor = TextPrimary,
                testTag = "telemetry_fps"
            )
            VibrantStatCard(
                modifier = Modifier.weight(1f),
                label = "LATENCY",
                value = "${state.latencyMs}",
                subText = "ms",
                valueColor = VibrantLatencyAlert,
                testTag = "telemetry_latency"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VibrantStatCard(
                modifier = Modifier.weight(1f),
                label = "FRAMES",
                value = if (state.framesDelivered > 1000) "%.1fk".format(state.framesDelivered / 1000.0) else "${state.framesDelivered}",
                subText = null,
                valueColor = TextPrimary,
                testTag = "telemetry_frames_del"
            )
            VibrantStatCard(
                modifier = Modifier.weight(1f),
                label = "RESOL",
                value = "${config.width}x${config.height}",
                subText = null,
                valueColor = TextPrimary,
                testTag = "telemetry_resol"
            )
        }
    }
}

@Composable
fun VibrantStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subText: String?,
    valueColor: Color,
    testTag: String
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        color = valueColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VibrantActionControls(
    systemState: KMJSSystemState,
    onNavigateToStream: () -> Unit,
    onNavigateToTests: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_controls_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CONFIGURATION & DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToStream,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("configure_stream_button"),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VibrantPurple)
                ) {
                    Text("Configure Stream", style = MaterialTheme.typography.labelMedium, color = VibrantPurple, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToTests,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_diagnostics_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple)
                ) {
                    Text("Run Diagnostics", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

