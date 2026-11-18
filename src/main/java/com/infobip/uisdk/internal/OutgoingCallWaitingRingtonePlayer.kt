package com.infobip.uisdk.internal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.infobip.uisdk.R

internal object OutgoingCallWaitingRingtonePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // No-op: WebRTC voice-call focus must not cancel local ringback.
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }

    fun start(context: Context) {
        val staleResources = synchronized(this) {
            if (mediaPlayer?.isPlaying == true) {
                return
            }
            detachPlayerResources()
        }
        releaseDetachedResources(staleResources)

        val appContext = context.applicationContext
        // Use media/notification usage so ringback stays audible while WebRTC holds the
        // voice-call stream (USAGE_VOICE_COMMUNICATION_SIGNALLING was effectively silent).
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        synchronized(this) {
            audioManager = manager
            requestAudioFocus(manager, attributes)
        }

        val customUri = InfobipCallSdkRegistry.config?.outgoingCallWaitingRingtoneUri
        if (customUri != null) {
            startFromUri(appContext, customUri, attributes)
        } else {
            startFromRawResource(appContext, attributes)
        }
    }

    fun stop() {
        val resources = synchronized(this) {
            detachPlayerResources()
        }
        releaseDetachedResources(resources)
    }

    private fun startFromUri(context: Context, uri: Uri, attributes: AudioAttributes) {
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(attributes)
            player.isLooping = true
            player.setDataSource(context, uri)
            player.prepare()
            player.start()
            synchronized(this) {
                mediaPlayer = player
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start custom outgoing waiting ringtone; trying SDK default", error)
            startFromRawResource(context, attributes)
        }
    }

    private fun startFromRawResource(context: Context, attributes: AudioAttributes) {
        try {
            val assetFd = context.resources.openRawResourceFd(R.raw.outgoing_waiting_ringtone)
            val player = MediaPlayer()
            player.setAudioAttributes(attributes)
            player.isLooping = true
            player.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
            assetFd.close()
            player.prepare()
            player.start()
            synchronized(this) {
                mediaPlayer = player
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start outgoing waiting ringtone", error)
            stop()
        }
    }

    private fun requestAudioFocus(manager: AudioManager, attributes: AudioAttributes) {
        // Do not stop on focus loss: WebRTC takes VOICE_CALL focus as soon as the call
        // starts, which would silence ringback if we treated LOSS as a stop signal.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    private fun detachPlayerResources(): PlayerResources {
        val resources = PlayerResources(
            player = mediaPlayer,
            manager = audioManager,
            focusRequest = audioFocusRequest,
        )
        mediaPlayer = null
        audioManager = null
        audioFocusRequest = null
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

        resources.manager?.let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resources.focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(audioFocusListener)
            }
        }
    }

    private data class PlayerResources(
        val player: MediaPlayer?,
        val manager: AudioManager?,
        val focusRequest: AudioFocusRequest?,
    )

    private const val TAG = "OutgoingWaitingRingtone"
}
