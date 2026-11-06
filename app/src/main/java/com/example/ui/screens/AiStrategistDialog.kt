package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TapUiState
import com.example.ui.TapViewModel
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary

@Composable
fun AiStrategistDialog(
    uiState: TapUiState,
    viewModel: TapViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (uiState.aiStrategyReport == null && !uiState.isGeneratingStrategy) {
            viewModel.fetchStrategyReport()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFFB388FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Quantitative Strategist",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = TapTextPrimary
                    )
                    Text(
                        text = "Powered by gemini-4-argon (ThinkingLevel.HIGH)",
                        fontSize = 10.sp,
                        color = Color(0xFFB388FF)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isGeneratingStrategy) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFB388FF))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Computing optimal booster ROI & upgrade path...",
                                fontSize = 12.sp,
                                color = TapTextSecondary
                            )
                        }
                    }
                } else if (uiState.aiStrategyReport != null) {
                    Surface(
                        color = TapSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.aiStrategyReport,
                            fontSize = 13.sp,
                            color = TapTextPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(14.dp)
                                .testTag("ai_strategy_report_text")
                        )
                    }
                } else {
                    Text(
                        text = "Tap Recompute to analyze your current balance, multitap level, and energy recovery rate.",
                        fontSize = 13.sp,
                        color = TapTextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.fetchStrategyReport() },
                enabled = !uiState.isGeneratingStrategy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Recompute", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Done", color = TapTextSecondary)
            }
        },
        containerColor = TapDarkCharcoal,
        shape = RoundedCornerShape(20.dp)
    )
}
