package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Enhanced audio and physical vibration/haptic feedback helper.
 * Provides tactile mechanical clicks and vibration impulses when tapping coins,
 * powering up turbo, collecting rewards, or encountering energy depletion.
 */
class AudioHapticHelper(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    var isSoundEnabled: Boolean = true
    var isHapticEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                @Suppress("DEPRECATION")
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Plays combined physical vibration/haptic feedback and audio tick on coin tap.
     */
    fun playTapFeedback(view: View? = null, isTurbo: Boolean = false) {
        if (isHapticEnabled) {
            triggerHaptic(view, isTurbo)
        }
        if (isSoundEnabled) {
            playTapSound(isTurbo)
        }
    }

    /**
     * Delivers a crisp, tactile physical vibration impulse.
     * Combines system view haptic feedback with direct hardware actuator vibration.
     */
    fun triggerHaptic(view: View? = null, isTurbo: Boolean = false) {
        if (!isHapticEnabled) return

        // 1. Perform immediate view haptic feedback
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            val feedbackConstant = if (isTurbo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                }
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view?.performHapticFeedback(feedbackConstant, flags)
        } catch (_: Exception) {}

        // 2. Hardware vibrator actuator pulse for a pronounced physical feel
        try {
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (isTurbo) {
                        // Sharp double-pulse haptic burst for Turbo mode
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 16, 26, 22),
                            intArrayOf(0, 220, 0, 255),
                            -1
                        )
                    } else {
                        // Crisp, punchy 22ms mechanical tactile click
                        VibrationEffect.createOneShot(22, 230)
                    }
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(if (isTurbo) 35 else 20)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Warning haptic pulse for depleted energy state.
     */
    fun triggerEnergyDepletedHaptic(view: View? = null) {
        if (!isHapticEnabled) return
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT, flags)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, flags)
            }
        } catch (_: Exception) {}

        try {
            val vib = vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Soft double-thump warning
                    vib.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 25, 45, 25),
                            intArrayOf(0, 110, 0, 110),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playTapSound(isTurbo: Boolean) {
        try {
            val tone = if (isTurbo) ToneGenerator.TONE_DTMF_D else ToneGenerator.TONE_PROP_BEEP
            toneGenerator?.startTone(tone, if (isTurbo) 40 else 25)
        } catch (_: Exception) {}
    }

    fun playCoinRewardSound() {
        if (isHapticEnabled) {
            try {
                val vib = vibrator
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 30, 40, 30, 40, 50),
                                intArrayOf(0, 180, 0, 220, 0, 255),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(100)
                    }
                }
            } catch (_: Exception) {}
        }

        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
