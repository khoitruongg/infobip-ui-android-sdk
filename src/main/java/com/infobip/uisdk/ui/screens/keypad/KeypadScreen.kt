package com.infobip.uisdk.ui.screens.keypad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infobip.uisdk.ui.components.CallBackground
import com.infobip.uisdk.ui.theme.currentCallAppearance
import com.infobip.uisdk.ui.theme.currentCallStrings

private val KeypadDigits = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("*", "0", "#"),
)

/**
 * In-call DTMF keypad. Hosts pass digits to the call via [onDigit] and dismiss with [onClose].
 */
@Composable
fun KeypadScreen(
    callerName: String,
    onDigit: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = currentCallAppearance
    val strings = currentCallStrings
    var entered by remember { mutableStateOf("") }

    CallBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = appearance.metrics.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = strings.keypad,
                style = appearance.fonts.title,
                color = appearance.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = callerName,
                style = appearance.fonts.caption,
                color = appearance.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = entered.ifBlank { " " },
                style = appearance.fonts.name,
                color = appearance.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))

            KeypadDigits.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { digit ->
                        KeypadKey(
                            digit = digit,
                            onClick = {
                                entered += digit
                                onDigit(digit)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            TextButton(onClick = onClose) {
                Text(
                    text = strings.close,
                    color = appearance.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(appearance.metrics.bottomSpacing))
        }
    }
}

@Composable
private fun KeypadKey(
    digit: String,
    onClick: () -> Unit,
) {
    val appearance = currentCallAppearance
    Box(
        modifier = Modifier
            .size(appearance.metrics.actionButtonDiameter)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit,
            style = appearance.fonts.name,
            color = appearance.colors.textPrimary,
        )
    }
}
