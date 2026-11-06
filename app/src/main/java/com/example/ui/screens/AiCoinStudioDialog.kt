package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TapUiState
import com.example.ui.TapViewModel
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapRedAlert
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary

@Composable
fun AiCoinStudioDialog(
    uiState: TapUiState,
    viewModel: TapViewModel,
    onDismiss: () -> Unit
) {
    var promptInput by remember { mutableStateOf("Cyberpunk dragon emblem with neon amber trim and metallic chrome accents") }
    var selectedResolution by remember { mutableStateOf("1K") } // 1K, 2K, 4K
    val resolutions = listOf("1K", "2K", "4K")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFB388FF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Generative Coin Studio",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TapTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Design a custom virtual 3D coin skin for your tapping screen powered by Gemini image generation.",
                    fontSize = 13.sp,
                    color = TapTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Resolution Affordance (1K, 2K, 4K)
                Text(
                    text = "IMAGE RESOLUTION QUALITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TapTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    resolutions.forEach { res ->
                        val isSelected = selectedResolution == res
                        Surface(
                            onClick = { selectedResolution = res },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TapGoldContainer else TapSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) TapGoldPrimary else TapOutline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("resolution_option_$res")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = res,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TapGoldLight else TapTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prompt Input
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Coin Theme Prompt") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_coin_prompt_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Preview or Loading Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TapSurfaceElevated)
                        .border(1.dp, TapOutline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isGeneratingAiCoin -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = TapGoldPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Synthesizing $selectedResolution coin (may take 30-45s)...",
                                    fontSize = 12.sp,
                                    color = TapTextSecondary
                                )
                            }
                        }
                        uiState.customCoinBitmap != null -> {
                            Image(
                                bitmap = uiState.customCoinBitmap.asImageBitmap(),
                                contentDescription = "Generated Coin",
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                            )
                        }
                        uiState.aiCoinError != null -> {
                            Text(
                                text = "Error: ${uiState.aiCoinError}",
                                color = TapRedAlert,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = "Preview will appear here after generation",
                                fontSize = 12.sp,
                                color = TapTextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        viewModel.generateCustomCoin(promptInput, selectedResolution)
                    }
                },
                enabled = !uiState.isGeneratingAiCoin,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("generate_coin_action_button")
            ) {
                Text(
                    text = if (uiState.isGeneratingAiCoin) "Generating..." else "Generate Coin ($selectedResolution)",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = TapTextSecondary)
            }
        },
        containerColor = TapDarkCharcoal,
        shape = RoundedCornerShape(20.dp)
    )
}
