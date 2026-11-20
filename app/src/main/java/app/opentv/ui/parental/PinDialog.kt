/*
 * This file is part of NKTV.
 */
package app.opentv.ui.parental

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.opentv.R
import app.opentv.core.ServiceLocator

/**
 * Guards a locked category. Call the returned function with the category key and what to do once
 * allowed: it runs [onGranted] straight away when the key needs no PIN, otherwise it raises the PIN
 * dialog and only runs it (and unlocks the category for this session) after the right PIN.
 */
@Composable
fun rememberPinGate(): (key: String, onGranted: () -> Unit) -> Unit {
    val context = LocalContext.current
    val settings = remember { ServiceLocator.get(context).settings }
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    pending?.let { (key, granted) ->
        PinDialog(
            verify = settings::verifyPin,
            onSuccess = {
                settings.unlockCategory(key)
                pending = null
                granted()
            },
            onDismiss = { pending = null },
        )
    }

    return { key, granted ->
        if (settings.needsPin(key)) pending = key to granted else granted()
    }
}

/**
 * A four-digit PIN prompt that works entirely from a remote: an on-screen keypad with visible focus,
 * plus the remote's own number keys. The fourth digit submits; a wrong PIN clears the entry and
 * says so, nothing is revealed until [verify] passes.
 */
@Composable
fun PinDialog(
    verify: (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }

    fun append(digit: Char) {
        if (pin.length >= 4) return
        pin += digit
        error = false
        if (pin.length == 4) {
            if (verify(pin)) onSuccess() else { pin = ""; error = true }
        }
    }

    fun backspace() {
        pin = pin.dropLast(1)
    }

    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.parental_enter_pin)) },
        text = {
            Column(
                Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = event.key.nativeKeyCode
                    when {
                        code in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> {
                            append('0' + (code - AndroidKeyEvent.KEYCODE_0)); true
                        }
                        code == AndroidKeyEvent.KEYCODE_DEL -> { backspace(); true }
                        else -> false
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < pin.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (error) stringResource(R.string.parental_wrong_pin) else " ",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("123", "456", "789").forEachIndexed { rowIndex, digits ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            digits.forEachIndexed { colIndex, d ->
                                PinKey(
                                    label = d.toString(),
                                    onClick = { append(d) },
                                    modifier = if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(firstKey) else Modifier,
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PinKey(label = "⌫", onClick = ::backspace)
                        PinKey(label = "0", onClick = { append('0') })
                        Spacer(Modifier.size(56.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(56.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
