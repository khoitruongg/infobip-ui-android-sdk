package com.infobip.uisdk.ui.screens.unreachable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infobip.uisdk.ui.components.CallAvatar
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings

/**
 * Outcome screen shown when the remote peer is unreachable / busy.
 *
 * Hosts wire [onRetry], [onSendMessage], and [onClose] to their own navigation.
 */
@Composable
fun UnreachableCallScreen(
    peerName: String,
    peerAvatarUrl: String? = null,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onSendMessage: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings

    CallBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = appearance.metrics.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CallAvatar(name = peerName, avatarUrl = peerAvatarUrl)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = strings.unreachableTitle,
                style = appearance.fonts.name,
                color = appearance.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle ?: strings.unreachableSubtitle,
                style = appearance.fonts.status,
                color = appearance.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = peerName,
                style = appearance.fonts.caption,
                color = appearance.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appearance.colors.accent,
                ),
            ) {
                Text(text = strings.tryAgain)
            }
            if (onSendMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onSendMessage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = strings.sendMessage)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClose) {
                Text(
                    text = strings.close,
                    color = appearance.colors.textSecondary,
                )
            }
        }
    }
}
