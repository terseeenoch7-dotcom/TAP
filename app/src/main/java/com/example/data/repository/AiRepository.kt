package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"

class AiRepository {

    // ------------------------------------------------------------------------
    // 1. High-Quality Generative Coin & Badge Images (1K, 2K, 4K affordance)
    // ------------------------------------------------------------------------
    suspend fun generateCollectorCoinImage(
        customPrompt: String,
        resolutionChoice: String // "1K", "2K", "4K"
    ): Result<Bitmap> {
        val selectedSize = when (resolutionChoice.uppercase()) {
            "4K" -> ImageSize.SIZE_4K
            "2K" -> ImageSize.SIZE_2K
            else -> ImageSize.SIZE_1K
        }

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setImageSize(selectedSize)
                .build()
        }

        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3-pro-image-preview",
            generationConfig = config
        )

        val fullPrompt = "A luxury high-detail circular 3D golden cryptocurrency game coin: $customPrompt, glowing rim lights, fintech collectible aesthetic, solid dark background"

        return try {
            val bitmap = withTimeout(60_000L) {
                val response = model.generateContent(fullPrompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No image was returned by Gemini."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation error", e)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // 2. High Thinking AI Tapping Strategy & Earnings Optimizer (gemini-4-argon)
    // ------------------------------------------------------------------------
    suspend fun getTapStrategyAdvice(
        coins: Long,
        totalTaps: Long,
        multitapLevel: Int,
        energyBoostLevel: Int,
        energyRecoveryLevel: Int,
        maxEnergy: Int
    ): Result<String> {
        val config = generationConfig {
            thinkingConfig = ThinkingConfig.Builder()
                .setThinkingLevel(ThinkingLevel.HIGH)
                .build()
        }

        val model = Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )

        val prompt = """
            You are the Chief Quantitative Game Strategist for the TAP fintech coin-tapping platform.
            Current Player Telemetry:
            - Current Coin Balance: $coins
            - Total Career Taps: $totalTaps
            - Multitap Level: $multitapLevel
            - Energy Boost Level: $energyBoostLevel
            - Energy Recovery Level: $energyRecoveryLevel
            - Current Max Energy: $maxEnergy

            Upgrade Economics:
            - Multitap upgrade cost: Level * 1,000 coins (+1 coin per tap)
            - Energy Boost upgrade cost: Level * 1,500 coins (+500 max capacity)
            - Energy Recovery upgrade cost: Level * 2,000 coins (+1 energy regen/sec)

            Conduct an in-depth mathematical optimization analysis:
            1. Calculate the Return on Investment (ROI) and payback period for each upgrade option.
            2. Determine the optimal capital allocation strategy for this player's current balance.
            3. Provide a clear, actionable, high-conviction 3-step recommendation on what to upgrade next.
            Keep the tone sharp, professional, and quantitative like an elite hedge-fund algorithmic report.
        """.trimIndent()

        return try {
            val responseText = withTimeout(60_000L) {
                val response = model.generateContent(prompt)
                response.text.orEmpty()
            }
            if (responseText.isNotBlank()) {
                Result.success(responseText)
            } else {
                Result.failure(IllegalStateException("Model returned an empty strategy report."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI strategy computation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI strategy computation failed", e)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // 3. Fast Admin Announcement & Task Generator (gemini-3.8-flash)
    // ------------------------------------------------------------------------
    suspend fun generateAnnouncement(topic: String): Result<Pair<String, String>> {
        val model = Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
        val prompt = "Generate a snappy announcement for a mobile tap-to-earn game about: '$topic'. Output exactly two lines: line 1 starting with 'TITLE: ', line 2 starting with 'BODY: '."

        return try {
            val text = withTimeout(30_000L) {
                model.generateContent(prompt).text.orEmpty()
            }
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val title = lines.find { it.startsWith("TITLE:", ignoreCase = true) }?.removePrefix("TITLE:")?.trim() ?: "Special Announcement"
            val body = lines.find { it.startsWith("BODY:", ignoreCase = true) }?.removePrefix("BODY:")?.trim() ?: text
            Result.success(Pair(title, body))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
