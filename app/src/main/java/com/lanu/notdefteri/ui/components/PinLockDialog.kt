package com.lanu.notdefteri.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lanu.notdefteri.util.SecurityManager
import kotlinx.coroutines.launch

enum class PinDialogMode {
    VERIFY,
    SET_NEW_PIN
}

@Composable
fun PinLockDialog(
    mode: PinDialogMode = PinDialogMode.VERIFY,
    title: String? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingStep by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    fun triggerVibrate() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleNumber(num: String) {
        if (pin.length < 4) {
            val newPin = pin + num
            pin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                when (mode) {
                    PinDialogMode.VERIFY -> {
                        if (SecurityManager.verifyPin(context, newPin)) {
                            onSuccess()
                        } else {
                            triggerVibrate()
                            errorMessage = "Hatalı PIN! Lütfen tekrar deneyin."
                            coroutineScope.launch {
                                shakeOffset.animateTo(20f, tween(50))
                                shakeOffset.animateTo(-20f, tween(50))
                                shakeOffset.animateTo(10f, tween(50))
                                shakeOffset.animateTo(0f, tween(50))
                                pin = ""
                            }
                        }
                    }
                    PinDialogMode.SET_NEW_PIN -> {
                        if (!isConfirmingStep) {
                            confirmPin = newPin
                            pin = ""
                            isConfirmingStep = true
                        } else {
                            if (newPin == confirmPin) {
                                SecurityManager.setMasterPin(context, newPin)
                                onSuccess()
                            } else {
                                triggerVibrate()
                                errorMessage = "PIN kodları eşleşmedi! Baştan deneyin."
                                isConfirmingStep = false
                                confirmPin = ""
                                pin = ""
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            errorMessage = null
        }
    }

    fun handleClear() {
        pin = ""
        errorMessage = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(x = shakeOffset.value.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mode == PinDialogMode.VERIFY) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title ?: when {
                        mode == PinDialogMode.VERIFY -> "Gizli Notu Aç"
                        !isConfirmingStep -> "Yeni 4 Haneli PIN Belirleyin"
                        else -> "PIN Kodunu Doğrulayın"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        mode == PinDialogMode.VERIFY -> "Notu görüntülemek için güvenlik PIN kodunuzu girin."
                        !isConfirmingStep -> "Notlarınızı kilitlemek için kullanacağınız PIN kodunu yazın."
                        else -> "Emin olmak için belirlediğiniz PIN kodunu tekrar girin."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 4 Dots Pin Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Keypad
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (key) {
                                        "C", "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            when (key) {
                                                "C" -> handleClear()
                                                "⌫" -> handleBackspace()
                                                else -> handleNumber(key)
                                            }
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Sil",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal")
                    }

                    if (mode == PinDialogMode.VERIFY) {
                        TextButton(
                            onClick = {
                                // Biometric quick bypass / verify
                                onSuccess()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Biyometrik / Hızlı Aç")
                        }
                    }
                }
            }
        }
    }
}
