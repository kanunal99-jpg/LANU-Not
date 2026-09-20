package com.lanu.notdefteri.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanu.notdefteri.util.AiTaskType
import com.lanu.notdefteri.util.GeminiAiHelper
import com.lanu.notdefteri.ui.viewmodel.NoteViewModel
import com.lanu.notdefteri.ai.DetectedEvent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantBottomSheet(
    currentContent: String,
    viewModel: NoteViewModel,
    onAppendResult: (String) -> Unit,
    onReplaceResult: (String) -> Unit,
    onEventSelected: (DetectedEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isDetecting.collectAsStateWithLifecycle()
    val detectedEvents by viewModel.detectedEvents.collectAsStateWithLifecycle()
    
    var localLoading by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<AiTaskType?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(GeminiAiHelper.getApiKey(context) ?: "") }

    fun executeAi(task: AiTaskType) {
        if (currentContent.isBlank()) {
            Toast.makeText(context, "Lütfen önce nota bir şeyler yazın.", Toast.LENGTH_SHORT).show()
            return
        }
        selectedTask = task
        
        if (task == AiTaskType.DETECT_EVENTS) {
            viewModel.detectEvents(currentContent)
            return
        }

        localLoading = true
        coroutineScope.launch {
            val res = GeminiAiHelper.processText(context, currentContent, task)
            localLoading = false
            res.onSuccess { text ->
                resultText = text
            }.onFailure { err ->
                Toast.makeText(context, "Hata: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Asistan (Gemini)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { showApiKeyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Anahtarı Ayarı",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Notunuzu tek dokunuşla özetleyin, dilbilgisini düzeltin veya yapılacaklar listesine çevirin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Task Actions Grid
            val tasks = listOf(
                AiTaskType.SUMMARIZE to Icons.Default.Summarize,
                AiTaskType.PROOFREAD to Icons.Default.Spellcheck,
                AiTaskType.CHECKLIST to Icons.Default.Checklist,
                AiTaskType.EXPAND to Icons.Default.Lightbulb,
                AiTaskType.DETECT_EVENTS to Icons.Default.CalendarToday
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((task, icon) in tasks) {
                    val isCurrent = selectedTask == task
                    Card(
                        onClick = { executeAi(task) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent && resultText != null)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.label,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading state
            AnimatedVisibility(visible = isLoading || localLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI notu işliyor...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Detected Events Result
            AnimatedVisibility(visible = !isLoading && detectedEvents.isNotEmpty() && selectedTask == AiTaskType.DETECT_EVENTS) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tespit Edilen Etkinlikler",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detectedEvents.forEach { event ->
                            Card(
                                onClick = { 
                                    onEventSelected(event)
                                    onDismiss()
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${event.date} ${event.time ?: ""}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.AddAlert,
                                        contentDescription = "Hatırlatıcı Ekle",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Hatırlatıcı oluşturmak için bir etkinliğe dokunun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Result preview box
            AnimatedVisibility(visible = !isLoading && !localLoading && resultText != null && selectedTask != AiTaskType.DETECT_EVENTS) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sonuç Önizleme",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Sonuç", resultText))
                                Toast.makeText(context, "Panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = resultText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                resultText?.let { onReplaceResult(it) }
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Metni Değiştir", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                resultText?.let { onAppendResult("\n\n" + it) }
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sona Ekle", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            icon = { Icon(Icons.Default.Key, contentDescription = null) },
            title = { Text("Gemini API Anahtarı") },
            text = {
                Column {
                    Text(
                        "Google AI Studio'dan aldığınız Gemini API anahtarını buraya ekleyebilirsiniz. Boş bırakırsanız akıllı yerel motor kullanılır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Anahtarı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        GeminiAiHelper.setApiKey(context, tempApiKey)
                        showApiKeyDialog = false
                        Toast.makeText(context, "API Anahtarı kaydedildi!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
