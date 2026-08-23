package com.kmjs.virtualcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.runtime.KMJSModuleLoader
import com.kmjs.virtualcamera.ui.DiagnosticTestScreen
import com.kmjs.virtualcamera.ui.DiagnosticsScreen
import com.kmjs.virtualcamera.ui.KMJSMainScreen
import com.kmjs.virtualcamera.ui.StreamConfigScreen
import com.kmjs.virtualcamera.ui.TargetManagementScreen
import com.kmjs.virtualcamera.ui.theme.KMJSVirtualCameraTheme
import com.kmjs.virtualcamera.ui.theme.TextMuted
import com.kmjs.virtualcamera.ui.theme.TextPrimary
import com.kmjs.virtualcamera.ui.theme.TextSecondary
import com.kmjs.virtualcamera.ui.theme.VibrantBackground
import com.kmjs.virtualcamera.ui.theme.VibrantCardBorder
import com.kmjs.virtualcamera.ui.theme.VibrantPurple
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleDark
import com.kmjs.virtualcamera.ui.theme.VibrantPurpleLight
import com.kmjs.virtualcamera.ui.theme.VibrantPurplePill
import com.kmjs.virtualcamera.ui.theme.VibrantSuccess

enum class KMJSTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Status", Icons.Default.Dashboard, "tab_dashboard"),
    STREAM("Stream", Icons.Default.Cast, "tab_stream"),
    TARGETS("Targets", Icons.AutoMirrored.Filled.ViewList, "tab_targets"),
    LOGS("Logs", Icons.AutoMirrored.Filled.ListAlt, "tab_logs"),
    TESTS("Tests", Icons.Default.Science, "tab_tests")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize KMJS Runtime & Diagnostics
        DiagnosticsLogger.module("KMJS Virtual Camera Application Launching (Process: ${packageName})")
        KMJSModuleLoader.startModule(classLoader)

        setContent {
            KMJSVirtualCameraTheme {
                KMJSAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KMJSAppContent() {
    var selectedTab by remember { mutableIntStateOf(KMJSTab.DASHBOARD.ordinal) }
    val systemState by StateRepository.systemState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VibrantBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KMJS",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VIRTUAL CAMERA V2.1",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    val isConnected = systemState.isConnected
                    val pillBg = if (isConnected) VibrantPurplePill else VibrantCardBorder
                    val pillText = if (isConnected) VibrantPurpleDark else TextSecondary
                    val dotColor = if (isConnected) VibrantPurpleDark else TextSecondary
                    val label = if (isConnected) "RUNNING" else "STANDBY"

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillBg,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = pillText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VibrantBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier
                    .border(width = 1.dp, color = VibrantCardBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("kmjs_bottom_nav")
            ) {
                KMJSTab.values().forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibrantPurpleDark,
                            selectedTextColor = VibrantPurpleDark,
                            indicatorColor = VibrantPurpleLight,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (KMJSTab.values()[selectedTab]) {
                KMJSTab.DASHBOARD -> KMJSMainScreen(
                    onNavigateToStream = { selectedTab = KMJSTab.STREAM.ordinal },
                    onNavigateToTests = { selectedTab = KMJSTab.TESTS.ordinal }
                )
                KMJSTab.STREAM -> StreamConfigScreen()
                KMJSTab.TARGETS -> TargetManagementScreen()
                KMJSTab.LOGS -> DiagnosticsScreen()
                KMJSTab.TESTS -> DiagnosticTestScreen()
            }
        }
    }
}

