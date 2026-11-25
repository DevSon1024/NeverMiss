package com.devson.NeverMiss.core.reminder.interval

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntervalTimerAlertPlayback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: IntervalTimerSettingsRepository
) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var timeoutJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    @Synchronized
    fun startAlert(customDurationMillis: Long? = null) {
        stopAlert()

        scope.launch {
            val settings = try {
                settingsRepository.settingsFlow.first()
            } catch (_: Throwable) {
                IntervalTimerSettings()
            }

            val durationMillis = customDurationMillis ?: (settings.alertDurationSeconds * 1000L)

            // Play sound if not silent
            if (settings.soundOption != AlertSoundOption.SILENT) {
                val soundUri: Uri? = when (settings.soundOption) {
                    AlertSoundOption.DEFAULT_ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    AlertSoundOption.DEFAULT_NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    AlertSoundOption.SILENT -> null
                } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                if (soundUri != null) {
                    audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    requestAudioFocus()

                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(context, soundUri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            isLooping = true
                            prepare()
                            start()
                        }
                    } catch (_: Throwable) {
                        // Fallback safely
                    }
                }
            }

            // Vibrate if enabled
            if (settings.vibrationEnabled) {
                try {
                    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }

                    val timings = settings.vibrationPattern.timings
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val amplitudes = IntArray(timings.size) { index -> if (index % 2 == 0) 0 else 255 }
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(timings, 0)
                    }
                } catch (_: Throwable) {
                    // Silently degrade if vibration is disabled or hardware missing
                }
            }

            // Automatic timeout
            timeoutJob = scope.launch {
                delay(durationMillis)
                stopAlert()
            }
        }
    }

    @Synchronized
    fun playPreview(soundOption: AlertSoundOption, vibrationPattern: VibrationPatternOption? = null) {
        stopAlert()

        if (soundOption != AlertSoundOption.SILENT) {
            val soundUri = when (soundOption) {
                AlertSoundOption.DEFAULT_ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                AlertSoundOption.DEFAULT_NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                AlertSoundOption.SILENT -> null
            } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (soundUri != null) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, soundUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = false
                        prepare()
                        start()
                    }
                } catch (_: Throwable) {
                }
            }
        }

        if (vibrationPattern != null) {
            try {
                val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = vibrationPattern.timings
                    val amplitudes = IntArray(timings.size) { if (it % 2 == 0) 0 else 255 }
                    vib?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
            } catch (_: Throwable) {
            }
        }

        timeoutJob = scope.launch {
            delay(5_000L) // Preview stops automatically after 5 seconds
            stopAlert()
        }
    }

    @Synchronized
    fun stopAlert() {
        timeoutJob?.cancel()
        timeoutJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Throwable) {
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (_: Throwable) {
        } finally {
            vibrator = null
        }

        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopAlert()
                    }
                }
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopAlert()
                    }
                },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }
}
