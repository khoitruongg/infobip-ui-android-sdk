package com.infobip.uisdk.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings
import kotlinx.coroutines.launch

@Composable
fun AcceptCallButton(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val strings = currentCallStrings
    val scope = rememberCoroutineScope()
    AcceptCallButton(
        onClick = { scope.launch { client.acceptIncomingCall() } },
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription ?: strings.acceptCallContentDescription,
        icon = icon,
        backgroundColor = backgroundColor,
        iconTint = iconTint,
    )
}

@Composable
fun AcceptCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    CallPrimaryActionButton(
        icon = icon ?: appearance.icons.accept,
        backgroundColor = backgroundColor ?: appearance.colors.accept,
        iconTint = iconTint,
        onClick = onClick,
        modifier = modifier,
        contentDescription = contentDescription ?: strings.acceptCallContentDescription,
        enabled = enabled,
    )
}

@Composable
fun DeclineCallButton(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val session by client.activeSession.collectAsStateWithLifecycle()
    val strings = currentCallStrings
    val scope = rememberCoroutineScope()
    val isActive = session?.state == CallState.Active
    DeclineCallButton(
        onClick = {
            scope.launch {
                if (isActive) {
                    client.hangUp()
                } else {
                    client.declineIncomingCall()
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        contentDescription = if (isActive) {
            strings.endCallContentDescription
        } else {
            contentDescription ?: strings.declineCallContentDescription
        },
        useCloseIcon = !isActive && session?.state == CallState.Incoming,
        icon = icon,
        backgroundColor = backgroundColor,
        iconTint = iconTint,
    )
}

@Composable
fun DeclineCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    useCloseIcon: Boolean = true,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    CallPrimaryActionButton(
        icon = icon ?: if (useCloseIcon) appearance.icons.decline else appearance.icons.hangUp,
        backgroundColor = backgroundColor ?: appearance.colors.decline,
        iconTint = iconTint,
        onClick = onClick,
        modifier = modifier,
        contentDescription = contentDescription ?: strings.declineCallContentDescription,
        enabled = enabled,
    )
}

@Composable
fun HangUpCallButton(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val strings = currentCallStrings
    val scope = rememberCoroutineScope()
    HangUpCallButton(
        onClick = { scope.launch { client.hangUp() } },
        modifier = modifier,
        enabled = enabled,
        contentDescription = contentDescription ?: strings.endCallContentDescription,
        icon = icon,
        backgroundColor = backgroundColor,
        iconTint = iconTint,
    )
}

@Composable
fun HangUpCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    CallPrimaryActionButton(
        icon = icon ?: appearance.icons.hangUp,
        backgroundColor = backgroundColor ?: appearance.colors.decline,
        iconTint = iconTint,
        onClick = onClick,
        modifier = modifier,
        contentDescription = contentDescription ?: strings.endCallContentDescription,
        enabled = enabled,
    )
}

@Composable
fun MicButton(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    onPress: (() -> Unit)? = null,
    icon: ImageVector? = null,
    activeIcon: ImageVector? = null,
    backgroundColor: Color? = null,
    activeBackgroundColor: Color? = null,
    iconTint: Color? = null,
    activeIconTint: Color? = null,
    label: String? = null,
    activeLabel: String? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
) {
    val session by client.activeSession.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context.findActivity()
    val strings = currentCallStrings
    var hasMicPermission by remember {
        mutableStateOf(context.hasRecordAudioPermission())
    }
    // After Android permanently denies RECORD_AUDIO, RequestPermission no longer shows a dialog.
    var needsMicSettings by remember { mutableStateOf(false) }
    var showMicSettingsDialog by remember { mutableStateOf(false) }

    fun refreshMicPermission() {
        hasMicPermission = context.hasRecordAudioPermission()
        if (hasMicPermission) {
            needsMicSettings = false
        }
    }

    fun isMicPermissionPermanentlyDenied(): Boolean {
        val currentActivity = activity ?: return false
        return !context.hasRecordAudioPermission() &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                currentActivity,
                Manifest.permission.RECORD_AUDIO,
            )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            needsMicSettings = false
            return@rememberLauncherForActivityResult
        }
        // Permanent denial: system dialog suppressed; prompt user to enable in Settings.
        if (isMicPermissionPermanentlyDenied()) {
            needsMicSettings = true
            showMicSettingsDialog = true
        }
    }

    LifecycleResumeEffect(Unit) {
        refreshMicPermission()
        onPauseOrDispose { }
    }

    if (showMicSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showMicSettingsDialog = false },
            title = { Text(strings.micPermissionTitle) },
            text = { Text(strings.micPermissionMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicSettingsDialog = false
                        context.openAppSettings()
                    },
                ) {
                    Text(strings.micPermissionConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicSettingsDialog = false }) {
                    Text(strings.micPermissionDismiss)
                }
            },
        )
    }

    val callControlsActive = session?.state == CallState.Active ||
        session?.state == CallState.Connecting ||
        session?.state == CallState.Ringing
    MicButton(
        isMuted = !hasMicPermission || session?.isMuted == true,
        onClick = {
            onPress?.invoke()
            refreshMicPermission()
            if (hasMicPermission) {
                scope.launch {
                    client.setMuted(session?.isMuted != true)
                }
                return@MicButton
            }
            if (needsMicSettings) {
                showMicSettingsDialog = true
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier,
        // Stay enabled without mic permission so the user can tap to enable it.
        enabled = if (hasMicPermission) enabled && callControlsActive else enabled,
        showLabel = showLabel,
        icon = icon,
        activeIcon = activeIcon,
        backgroundColor = backgroundColor,
        activeBackgroundColor = activeBackgroundColor,
        iconTint = iconTint,
        activeIconTint = activeIconTint,
        label = label,
        activeLabel = activeLabel,
        labelStyle = labelStyle,
        labelColor = labelColor,
    )
}

@Composable
fun MicButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    icon: ImageVector? = null,
    activeIcon: ImageVector? = null,
    backgroundColor: Color? = null,
    activeBackgroundColor: Color? = null,
    iconTint: Color? = null,
    activeIconTint: Color? = null,
    label: String? = null,
    activeLabel: String? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val resolvedLabel = if (isMuted) {
        activeLabel ?: strings.unmute
    } else {
        label ?: strings.mute
    }
    val muteIcon = icon ?: appearance.icons.mute
    val unmuteIcon = activeIcon ?: appearance.icons.unmute
    val inactiveBackground = backgroundColor ?: appearance.colors.controlInactiveBackground
    val mutedBackground = activeBackgroundColor ?: appearance.colors.micActiveBackground
    val inactiveTint = iconTint ?: appearance.colors.iconInactive
    val mutedTint = activeIconTint ?: appearance.colors.iconOnControl
    if (showLabel) {
        CallControlItem(
            label = resolvedLabel,
            icon = muteIcon,
            activeIcon = unmuteIcon,
            activeBackgroundColor = mutedBackground,
            activeIconTint = mutedTint,
            inactiveBackgroundColor = inactiveBackground,
            inactiveIconTint = inactiveTint,
            labelStyle = labelStyle,
            labelColor = labelColor,
            isActive = isMuted,
            enabled = enabled,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        CallSecondaryActionButton(
            icon = if (isMuted) unmuteIcon else muteIcon,
            backgroundColor = if (isMuted) mutedBackground else inactiveBackground,
            iconTint = if (isMuted) mutedTint else inactiveTint,
            onClick = onClick,
            enabled = enabled,
            contentDescription = resolvedLabel,
            modifier = modifier,
        )
    }
}

@Composable
fun SpeakerButton(
    client: InfobipCallClient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    onPress: (() -> Unit)? = null,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    activeBackgroundColor: Color? = null,
    iconTint: Color? = null,
    activeIconTint: Color? = null,
    label: String? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
) {
    val session by client.activeSession.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    SpeakerButton(
        isSpeakerOn = session?.isSpeakerOn == true,
        onClick = {
            onPress?.invoke()
            scope.launch {
                client.setSpeakerOn(session?.isSpeakerOn != true)
            }
        },
        modifier = modifier,
        enabled = enabled && (session?.state == CallState.Active ||
            session?.state == CallState.Connecting ||
            session?.state == CallState.Ringing),
        showLabel = showLabel,
        icon = icon,
        backgroundColor = backgroundColor,
        activeBackgroundColor = activeBackgroundColor,
        iconTint = iconTint,
        activeIconTint = activeIconTint,
        label = label,
        labelStyle = labelStyle,
        labelColor = labelColor,
    )
}

@Composable
fun SpeakerButton(
    isSpeakerOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    activeBackgroundColor: Color? = null,
    iconTint: Color? = null,
    activeIconTint: Color? = null,
    label: String? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val resolvedLabel = label ?: strings.speaker
    val speakerIcon = icon ?: appearance.icons.speaker
    val inactiveBackground = backgroundColor ?: appearance.colors.controlInactiveBackground
    val activeBackground = activeBackgroundColor ?: appearance.colors.controlActiveBackground
    val inactiveTint = iconTint ?: appearance.colors.iconInactive
    val activeTint = activeIconTint ?: appearance.colors.iconOnControl
    if (showLabel) {
        CallControlItem(
            label = resolvedLabel,
            icon = speakerIcon,
            activeBackgroundColor = activeBackground,
            activeIconTint = activeTint,
            inactiveBackgroundColor = inactiveBackground,
            inactiveIconTint = inactiveTint,
            labelStyle = labelStyle,
            labelColor = labelColor,
            isActive = isSpeakerOn,
            enabled = enabled,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        CallSecondaryActionButton(
            icon = speakerIcon,
            backgroundColor = if (isSpeakerOn) activeBackground else inactiveBackground,
            iconTint = if (isSpeakerOn) activeTint else inactiveTint,
            onClick = onClick,
            enabled = enabled,
            contentDescription = resolvedLabel,
            modifier = modifier,
        )
    }
}

@Composable
fun MessageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
    label: String? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    val resolvedLabel = label ?: strings.message
    val messageIcon = icon ?: appearance.icons.message
    val resolvedBackground = backgroundColor ?: appearance.colors.controlInactiveBackground
    val resolvedTint = iconTint ?: appearance.colors.iconInactive
    if (showLabel) {
        CallControlItem(
            label = resolvedLabel,
            icon = messageIcon,
            inactiveBackgroundColor = resolvedBackground,
            inactiveIconTint = resolvedTint,
            labelStyle = labelStyle,
            labelColor = labelColor,
            enabled = enabled,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        CallSecondaryActionButton(
            icon = messageIcon,
            backgroundColor = resolvedBackground,
            iconTint = resolvedTint,
            onClick = onClick,
            enabled = enabled,
            contentDescription = resolvedLabel,
            modifier = modifier,
        )
    }
}

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
