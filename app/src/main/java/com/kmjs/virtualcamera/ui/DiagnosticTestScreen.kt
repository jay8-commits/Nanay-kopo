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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmjs.virtualcamera.testing.DiagnosticTestItem
import com.kmjs.virtualcamera.testing.DiagnosticTestRunner
import com.kmjs.virtualcamera.testing.TestStatus
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
fun DiagnosticTestScreen(
    modifier: Modifier = Modifier
) {
    val testItems by DiagnosticTestRunner.testItemsFlow.collectAsStateWithLifecycle()

    val passCount = testItems.count { it.status == TestStatus.PASS }
    val failCount = testItems.count { it.status == TestStatus.FAIL }
    val totalCount = testItems.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LAYER ISOLATION TESTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibrantPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Diagnostic Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Summary Score Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = if (passCount == totalCount && totalCount > 0) VibrantSuccess.copy(alpha = 0.15f) else VibrantPurplePill,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (passCount == totalCount && totalCount > 0) VibrantSuccess else VibrantPurple)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$passCount / $totalCount PASS",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (passCount == totalCount && totalCount > 0) VibrantSuccess else VibrantPurpleDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { DiagnosticTestRunner.runAllTests() },
                modifier = Modifier.weight(1f).testTag("btn_run_all_diagnostics"),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RUN ALL TESTS", color = Color.White, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { DiagnosticTestRunner.reset() },
                modifier = Modifier.testTag("btn_reset_tests"),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, VibrantCardBorder)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", color = TextSecondary)
            }
        }

        // Test Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(testItems, key = { it.id }) { item ->
                DiagnosticItemCard(item = item)
            }
        }
    }
}

@Composable
fun DiagnosticItemCard(item: DiagnosticTestItem) {
    val statusColor = when (item.status) {
        TestStatus.PASS -> VibrantSuccess
        TestStatus.FAIL -> VibrantLatencyAlert
        TestStatus.RUNNING -> VibrantPurple
        TestStatus.NOT_TESTED -> TextMuted
    }

    val statusBg = when (item.status) {
        TestStatus.PASS -> VibrantSuccess.copy(alpha = 0.12f)
        TestStatus.FAIL -> VibrantLatencyAlert.copy(alpha = 0.12f)
        TestStatus.RUNNING -> VibrantPurplePill
        TestStatus.NOT_TESTED -> VibrantBackground
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("test_layer_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.status == TestStatus.PASS) VibrantSuccess.copy(alpha = 0.4f) else VibrantCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        shape = CircleShape,
                        color = VibrantPurplePill,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${item.id}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VibrantPurpleDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.status.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = item.details.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkConsoleBackground
                ) {
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == TestStatus.FAIL) VibrantLatencyAlert else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

