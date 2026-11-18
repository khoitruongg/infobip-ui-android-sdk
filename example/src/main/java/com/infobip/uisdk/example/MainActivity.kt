package com.infobip.uisdk.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.infobip.uisdk.InfobipCallClient
import com.infobip.uisdk.InfobipCallSdk
import com.infobip.uisdk.example.customui.CustomIncomingCallScreen
import com.infobip.uisdk.example.customui.CustomOutgoingCallScreen
import com.infobip.uisdk.model.CallState
import com.infobip.uisdk.push.InfobipCallIntents
import com.infobip.uisdk.push.InfobipCallPush
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* Denied permissions can be handled by the host app */ }

    private val callClient by lazy { (application as ExampleApplication).callClient }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCallPermissions()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )

        handleIncomingCallIntent(intent)

        setContent {
            // Host branding: see ExampleCallTheme for a full CallAppearance / CallStrings sample.
            InfobipCallSdk.Theme(
                appearance = ExampleCallTheme.appearance,
                strings = ExampleCallTheme.strings,
            ) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        SimpleExampleScreen(
                            modifier = Modifier.fillMaxSize(),
                            useTokenProvider = (application as ExampleApplication).useTokenProvider,
                            onRegister = { identity, displayName ->
                                val token = InfobipTokenClient.fetchToken(identity, displayName)
                                TokenStore.token = token
                                TokenStore.displayName = displayName
                                callClient.registerSubscriber(
                                    identity = identity,
                                    displayName = displayName,
                                    token = token,
                                )
                                // Failures propagate — SimpleExampleScreen shows registerError.
                                callClient.registerForIncomingCalls()
                                Toast.makeText(
                                    this@MainActivity,
                                    "Registered for incoming calls",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onUnregister = {
                                TokenStore.clear()
                                // Clears RTC, push, and the persisted incoming-calls gate.
                                callClient.clearSubscriber()
                            },
                            callClient = callClient,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingCallIntent(intent)
    }

    private fun handleIncomingCallIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            InfobipCallIntents.ACTION_ACCEPT -> {
                InfobipCallPush.dismissIncomingCallNotification(this)
                lifecycleScope.launch {
                    callClient.acceptIncomingCall()
                }
                intent.action = null
            }
        }

        if (intent.getBooleanExtra(InfobipCallIntents.EXTRA_INCOMING_CALL, false) ||
            intent.getBooleanExtra(InfobipCallIntents.EXTRA_OPEN_INCOMING_CALL, false)
        ) {
            InfobipCallPush.cancelIncomingCallNotification(this)
        }
    }

    private fun requestCallPermissions() {
        val requiredPermissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (requiredPermissions.isNotEmpty()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
}

@Composable
private fun SimpleExampleScreen(
    callClient: InfobipCallClient,
    onRegister: suspend (identity: String, displayName: String) -> Unit,
    onUnregister: () -> Unit,
    useTokenProvider: Boolean,
    modifier: Modifier = Modifier,
) {
    var identity by remember { mutableStateOf("alice") }
    var displayName by remember { mutableStateOf("Alice") }
    val defaultAvatarUrl = remember(identity) { "https://i.pravatar.cc/256?u=${identity.trim()}" }
    var avatarUrl by remember { mutableStateOf(defaultAvatarUrl) }
    var avatarUrlEdited by remember { mutableStateOf(false) }
    var destinationIdentity by remember { mutableStateOf("bob") }
    var destinationDisplayName by remember { mutableStateOf("Bob") }
    var isRegistering by remember { mutableStateOf(false) }
    var registerError by remember { mutableStateOf<String?>(null) }

    var outgoingTo by remember { mutableStateOf<String?>(null) }
    var outgoingDisplayName by remember { mutableStateOf<String?>(null) }
    val session by callClient.activeSession.collectAsState(initial = null)

    val callState = session?.state
    val showIncoming = callState == CallState.Incoming || callState == CallState.Active
    val canRegister = identity.isNotBlank() && displayName.isNotBlank() && !isRegistering

    LaunchedEffect(session?.state, session?.endCode, session?.endedBy, session?.failureReason) {
        Log.d(
            "SimpleExampleScreen",
            "session.state=${session?.state}" +
                " endCode=${session?.endCode?.name}(${session?.endCode?.id})" +
                " endedBy=${session?.endedBy}" +
                " failureReason=${session?.failureReason}",
        )
    }

    val scope = rememberCoroutineScope()

    when {
        outgoingTo != null -> {
            CustomOutgoingCallScreen(
                client = callClient,
                destinationIdentity = outgoingTo.orEmpty(),
                destinationDisplayName = outgoingDisplayName,
                customData = buildMap {
                    displayName.trim().takeIf { it.isNotBlank() }?.let { put("userName", it) }
                    val resolvedAvatarUrl = avatarUrl.trim().ifBlank { defaultAvatarUrl }
                    put("avatarUrl", resolvedAvatarUrl)
                },
                onCallEnded = {
                    outgoingTo = null
                    outgoingDisplayName = null
                },
                onCallFailed = {
                    outgoingTo = null
                    outgoingDisplayName = null
                },
            )
        }
        showIncoming -> {
            CustomIncomingCallScreen(
                client = callClient,
                onCallEnded = { /* return to home UI automatically */ },
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Infobip UI SDK – simplest integration",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (useTokenProvider) {
                            "tokenProvider: ON (auto-refresh)"
                        } else {
                            "tokenProvider: OFF (registerSubscriber token only)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Enter your identity and tap register.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = identity,
                        onValueChange = {
                            identity = it
                            registerError = null
                            if (!avatarUrlEdited) {
                                avatarUrl = "https://i.pravatar.cc/256?u=${it.trim()}"
                            }
                        },
                        label = { Text("Your identity") },
                        singleLine = true,
                        enabled = !isRegistering,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            registerError = null
                        },
                        label = { Text("Your display name") },
                        singleLine = true,
                        enabled = !isRegistering,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = avatarUrl,
                        onValueChange = {
                            avatarUrl = it
                            avatarUrlEdited = true
                        },
                        label = { Text("Your avatar URL") },
                        singleLine = true,
                        enabled = !isRegistering,
                    )

                    registerError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            registerError = null
                            isRegistering = true
                            scope.launch {
                                try {
                                    onRegister(identity.trim(), displayName.trim())
                                } catch (e: Exception) {
                                    registerError = e.message ?: "Registration failed."
                                } finally {
                                    isRegistering = false
                                }
                            }
                        },
                        enabled = canRegister,
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator()
                        } else {
                            Text("Register for incoming calls", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = destinationIdentity,
                        onValueChange = { destinationIdentity = it },
                        label = { Text("Call this identity") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = destinationDisplayName,
                        onValueChange = { destinationDisplayName = it },
                        label = { Text("Callee display name") },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        Button(
                            onClick = {
                                val to = destinationIdentity.trim()
                                if (to.isNotBlank()) {
                                    outgoingTo = to
                                    outgoingDisplayName = destinationDisplayName.trim()
                                        .takeIf { it.isNotBlank() }
                                }
                            },
                            enabled = destinationIdentity.isNotBlank(),
                        ) {
                            Text("Start outgoing call", color = Color.White)
                        }
                        Button(onClick = onUnregister) {
                            Text("Unregister", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
