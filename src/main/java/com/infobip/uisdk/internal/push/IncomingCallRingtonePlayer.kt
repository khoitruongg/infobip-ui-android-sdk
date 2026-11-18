package com.infobip.uisdk.internal.push

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import com.infobip.uisdk.internal.InfobipCallSdkRegistry

internal object IncomingCallRingtonePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var previousStreamVolume: Int? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> stop()
        }
    }

    fun start(context: Context) {
        val staleResources = synchronized(this) {
            if (mediaPlayer?.isPlaying == true) {
                return
            }
            detachPlayerResources()
        }
        releaseDetachedResources(staleResources)

        val appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        synchronized(this) {
            audioManager = manager
            // Volume / focus / wake lock must never abort incoming-call delivery
            // (e.g. SecurityException when Do Not Disturb blocks STREAM_RING changes).
            runCatching { setStreamToMaxVolume(manager) }
                .onFailure { Log.w(TAG, "Failed to raise ringtone stream volume", it) }
            runCatching { requestAudioFocus(manager, attributes) }
                .onFailure { Log.w(TAG, "Failed to request ringtone audio focus", it) }
            runCatching { acquireWakeLock(appContext) }
                .onFailure { Log.w(TAG, "Failed to acquire ringtone wake lock", it) }
            startVibration(appContext)
        }

        val ringtoneUri = resolveRingtoneUri(appContext)
        if (ringtoneUri == null) {
            Log.w(TAG, "No default ringtone URI available; vibration only")
            return
        }

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(attributes)
            player.isLooping = true
            player.setVolume(MEDIA_PLAYER_MAX_VOLUME, MEDIA_PLAYER_MAX_VOLUME)
            player.setDataSource(appContext, ringtoneUri)
            player.prepare()
            player.start()
            synchronized(this) {
                mediaPlayer = player
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start incoming call ringtone", error)
            stop()
        }
    }

    fun stop() {
        val resources = synchronized(this) {
            detachPlayerResources()
        }
        releaseDetachedResources(resources)
    }

    private fun resolveRingtoneUri(context: Context): Uri? {
        InfobipCallSdkRegistry.config?.incomingCallRingtoneUri?.let { return it }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_RINGTONE_URI
            ?: runCatching {
                RingtoneManager(context).getRingtoneUri(0)
            }.getOrNull()
    }

    private fun startVibration(context: Context) {
        val vib = resolveVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(
                    VibrationEffect.createWaveform(VIBRATION_PATTERN, /* repeatIndex */ 0),
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(VIBRATION_PATTERN, /* repeat */ 0)
            }
            vibrator = vib
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start incoming call vibration", error)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun setStreamToMaxVolume(manager: AudioManager) {
        previousStreamVolume = manager.getStreamVolume(PLAYBACK_STREAM)
        val maxVolume = manager.getStreamMaxVolume(PLAYBACK_STREAM)
        // Throws SecurityException when Do Not Disturb / volume policy blocks changes.
        manager.setStreamVolume(PLAYBACK_STREAM, maxVolume, 0)
    }

    private fun requestAudioFocus(manager: AudioManager, attributes: AudioAttributes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusListener,
                PLAYBACK_STREAM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    private fun acquireWakeLock(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "InfobipUiSdk:IncomingCallRingtone",
        ).apply {
            acquire(RINGTONE_WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun detachPlayerResources(): PlayerResources {
        val resources = PlayerResources(
            player = mediaPlayer,
            manager = audioManager,
            focusRequest = audioFocusRequest,
            lock = wakeLock,
            vib = vibrator,
            savedStreamVolume = previousStreamVolume,
        )
        mediaPlayer = null
        audioManager = null
        audioFocusRequest = null
        wakeLock = null
        vibrator = null
        previousStreamVolume = null
        return resources
    }

    private fun releaseDetachedResources(resources: PlayerResources) {
        resources.player?.run {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (_: Exception) {
            }
            release()
        }

        resources.vib?.cancel()

        resources.manager?.let { manager ->
            resources.savedStreamVolume?.let { volume ->
                runCatching { manager.setStreamVolume(PLAYBACK_STREAM, volume, 0) }
                    .onFailure { Log.w(TAG, "Failed to restore ringtone stream volume", it) }
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    resources.focusRequest?.let { manager.abandonAudioFocusRequest(it) }
                } else {
                    @Suppress("DEPRECATION")
                    manager.abandonAudioFocus(audioFocusListener)
                }
            }.onFailure { Log.w(TAG, "Failed to abandon ringtone audio focus", it) }
        }

        resources.lock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
    }

    private data class PlayerResources(
        val player: MediaPlayer?,
        val manager: AudioManager?,
        val focusRequest: AudioFocusRequest?,
        val lock: PowerManager.WakeLock?,
        val vib: Vibrator?,
        val savedStreamVolume: Int?,
    )

    private const val TAG = "IncomingCallRingtone"
    private const val PLAYBACK_STREAM = AudioManager.STREAM_RING
    private const val MEDIA_PLAYER_MAX_VOLUME = 1f
    private const val RINGTONE_WAKE_LOCK_TIMEOUT_MS = 60_000L
    private val VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)
}
