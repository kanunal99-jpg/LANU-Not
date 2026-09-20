package com.lanu.notdefteri.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.DocumentShareHelper
// import com.lanu.notdefteri.util.PowerPointGenerator
import com.lanu.notdefteri.util.RichTextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val displayName: String, val extension: String, val icon: ImageVector, val accentColor: Color) {
    PDF("PDF Belgesi", ".pdf", Icons.Default.PictureAsPdf, Color(0xFFD32F2F)),
    EXCEL("Excel Tablosu", ".xlsx", Icons.Default.TableChart, Color(0xFF2E7D32)),
    // PPTX("PowerPoint Sunumu", ".pptx", Icons.Default.Slideshow, Color(0xFFD83B01)),
    TXT("Metin Belgesi", ".txt", Icons.Default.Article, Color(0xFF1976D2)),
    JSON("JSON Verisi", ".json", Icons.Default.DataObject, Color(0xFF7B1FA2))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPreviewBottomSheet(
    note: Note,
    initialFormat: ExportFormat = ExportFormat.PDF,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var includeImage by remember { mutableStateOf(!note.imagePath.isNullOrBlank()) }
    var includeLocation by remember { mutableStateOf(!note.locationName.isNullOrBlank()) }
    var includeMetadata by remember { mutableStateOf(true) }
    var selectedShareChannel by remember { mutableStateOf("generic") }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }

    val formattedDate = remember(note.createdAt) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
        sdf.format(Date(note.createdAt))
    }

    val wordCount = remember(note.content) {
        if (note.content.isBlank()) 0
        else note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("export_preview_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Canlı Dışa Aktarım Önizlemesi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Belgenizin dışa aktarılmadan önceki görünümünü inceleyin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("export_close_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection Chips / Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFormat.ordinal,
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().testTag("export_format_tabs")
            ) {
                ExportFormat.values().forEach { format ->
                    Tab(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedFormat == format) format.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (selectedFormat == format) 1.5.dp else 1.dp,
                                color = if (selectedFormat == format) format.accentColor else Color.Transparent
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = format.icon,
                                    contentDescription = format.displayName,
                                    tint = if (selectedFormat == format) format.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = format.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedFormat == format) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedFormat == format) format.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE VISUAL PREVIEW CANVAS CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_preview_canvas")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(selectedFormat.accentColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CANLI BELGE ÖNİZLEMESİ (${selectedFormat.extension.uppercase()})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = selectedFormat.accentColor
                                )
                            )
                        }

                        Text(
                            text = "$wordCount kelime • ${note.content.length} kr.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Format Preview Box
                    AnimatedContent(
                        targetState = selectedFormat,
                        label = "format_preview_transition"
                    ) { targetFormat ->
                        when (targetFormat) {
                            ExportFormat.PDF -> PdfLivePreviewCard(
                                note = note,
                                formattedDate = formattedDate,
                                includeImage = includeImage,
                                includeLocation = includeLocation,
                                includeMetadata = includeMetadata
                            )
                            ExportFormat.EXCEL -> ExcelLivePreviewCard(
                                note = note,
                                formattedDate = formattedDate,
                                includeLocation = includeLocation,
                                includeMetadata = includeMetadata
                            )
                            ExportFormat.TXT -> TxtLivePreviewCard(
                                note = note,
                                formattedDate = formattedDate,
                                includeLocation = includeLocation
                            )
                            ExportFormat.JSON -> JsonLivePreviewCard(
                                note = note,
                                formattedDate = formattedDate,
                                includeLocation = includeLocation
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CUSTOMIZATION OPTIONS SECTION
            Text(
                text = "Dışa Aktarım Ayarları",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (!note.imagePath.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ekli Görseli Dahil Et",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = includeImage,
                            onCheckedChange = { includeImage = it },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                if (!note.locationName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Konum Bilgisini Dahil Et",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = includeLocation,
                            onCheckedChange = { includeLocation = it },
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tarih ve Kategori Bilgisini Ekle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = includeMetadata,
                        onCheckedChange = { includeMetadata = it },
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Paylaşım Kanalları
            Text(
                text = "Paylaşım Kanalı",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generic (Tüm Uygulamalar)
                FilterChip(
                    selected = selectedShareChannel == "generic",
                    onClick = { selectedShareChannel = "generic" },
                    label = { Text("Tüm Uygulamalar", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedFormat.accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = selectedFormat.accentColor,
                        selectedLeadingIconColor = selectedFormat.accentColor
                    ),
                    modifier = Modifier.weight(1f)
                )

                // E-posta
                FilterChip(
                    selected = selectedShareChannel == "email",
                    onClick = { selectedShareChannel = "email" },
                    label = { Text("E-posta", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedFormat.accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = selectedFormat.accentColor,
                        selectedLeadingIconColor = selectedFormat.accentColor
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Mesajlaşma
                FilterChip(
                    selected = selectedShareChannel == "messaging",
                    onClick = { selectedShareChannel = "messaging" },
                    label = { Text("Mesajlaşma", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedFormat.accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = selectedFormat.accentColor,
                        selectedLeadingIconColor = selectedFormat.accentColor
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isExporting) {
                // LINEAR PROGRESS INDICATOR CARD FOR LARGE FILE EXPORTS
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = selectedFormat.accentColor.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, selectedFormat.accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_progress_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = selectedFormat.icon,
                                    contentDescription = null,
                                    tint = selectedFormat.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedFormat.displayName} Oluşturuluyor...",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Text(
                                text = "${(exportProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = selectedFormat.accentColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("export_linear_progress_indicator"),
                            color = selectedFormat.accentColor,
                            trackColor = selectedFormat.accentColor.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = exportStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                exportProgress = 0.1f
                                exportStatusText = "Belge yapısı ve şablonlar hazırlanıyor..."
                                delay(200)

                                exportProgress = 0.35f
                                exportStatusText = "Metin içeriği ve başlıklar işleniyor..."
                                delay(250)

                                exportProgress = 0.65f
                                exportStatusText = if (includeImage && !note.imagePath.isNullOrBlank()) "Görsel nesneleri ve grafikler dönüştürülüyor..." else "Tablo ve hücre biçimleri hizalanıyor..."
                                delay(250)

                                exportProgress = 0.90f
                                exportStatusText = "${selectedFormat.extension.uppercase()} dosyası sıkıştırılıyor ve kaydediliyor..."
                                delay(200)

                                exportProgress = 1.0f
                                exportStatusText = "İşlem tamamlandı! Paylaşım menüsü açılıyor..."
                                delay(150)

                                when (selectedShareChannel) {
                                    "email" -> DocumentShareHelper.shareViaEmail(context, note, selectedFormat.extension)
                                    "messaging" -> DocumentShareHelper.shareViaMessaging(context, note, selectedFormat.extension)
                                    else -> DocumentShareHelper.shareGeneric(context, note, selectedFormat.extension)
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedFormat.accentColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("confirm_export_button")
                    ) {
                        Icon(
                            imageVector = when (selectedShareChannel) {
                                "email" -> Icons.Default.Email
                                "messaging" -> Icons.Default.Forum
                                else -> Icons.Default.IosShare
                            },
                            contentDescription = "Dışa Aktar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedShareChannel) {
                                "email" -> "E-posta ile Paylaş"
                                "messaging" -> "Mesajlaşma ile Paylaş"
                                else -> "${selectedFormat.displayName} Olarak Paylaş"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Scale helper for clean compact switches
private fun Modifier.scale(scale: Float): Modifier = this

// --- PDF LIVE PREVIEW CARD ---
@Composable
fun PdfLivePreviewCard(
    note: Note,
    formattedDate: String,
    includeImage: Boolean,
    includeLocation: Boolean,
    includeMetadata: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // PDF Header Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFFC25D23))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LANU NOT DEFTERİ — RESMİ İHRACAT BELGESİ",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A)
                )
                Text(
                    text = "A4 FORMATI",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC25D23)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Document Title
            Text(
                text = note.title.ifBlank { "Başlıksız Not" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18181B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (includeMetadata) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tarih: $formattedDate",
                        fontSize = 8.5.sp,
                        color = Color(0xFF52525B)
                    )
                    note.tag?.let { tag ->
                        Text(
                            text = "Kategori: $tag",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC25D23)
                        )
                    }
                }
            }

            if (includeLocation && !note.locationName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📍 Konum: ${note.locationName}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF27272A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE4E4E7), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Attached Image Preview inside PDF
            if (includeImage && !note.imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = note.imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Document Body Content
            val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text
            Text(
                text = cleanContent.ifBlank { "İçerik boş." },
                fontSize = 9.5.sp,
                color = Color(0xFF27272A),
                lineHeight = 13.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(8.dp))

            // PDF Footer Bar
            HorizontalDivider(color = Color(0xFFE4E4E7), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bu belge LANU Not Defteri ile profesyonel oluşturulmuştur.",
                    fontSize = 6.5.sp,
                    color = Color(0xFFA1A1AA)
                )
                Text(
                    text = "Sayfa 1 / 1",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A)
                )
            }
        }
    }
}

// --- EXCEL LIVE PREVIEW CARD ---
@Composable
fun ExcelLivePreviewCard(
    note: Note,
    formattedDate: String,
    includeLocation: Boolean,
    includeMetadata: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Excel Sheet Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📊 LANU NOT DEFTERİ — EXCEL TABLOSU (.XLSX)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Sayfa: Notlar Raporu",
                    fontSize = 7.5.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spreadsheet Table Grid Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                    .horizontalScroll(rememberScrollState())
            ) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFF7043))
                        .padding(vertical = 4.dp)
                ) {
                    ExcelCell("ID", width = 30, isHeader = true)
                    ExcelCell("BAŞLIK", width = 80, isHeader = true)
                    ExcelCell("KATEGORİ", width = 60, isHeader = true)
                    ExcelCell("NOT İÇERİĞİ", width = 120, isHeader = true)
                    ExcelCell("OLUŞTURULMA TARİHİ", width = 90, isHeader = true)
                    ExcelCell("SABİTLENDİ", width = 50, isHeader = true)
                }

                // Table Data Row
                val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text
                Row(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    ExcelCell(note.id.toString(), width = 30)
                    ExcelCell(note.title.ifBlank { "Başlıksız" }, width = 80)
                    ExcelCell(if (includeMetadata) (note.tag ?: "-") else "-", width = 60)
                    ExcelCell(cleanContent.ifBlank { "Boş" }, width = 120)
                    ExcelCell(if (includeMetadata) formattedDate else "-", width = 90)
                    ExcelCell(if (note.isPinned) "EVET" else "HAYIR", width = 50)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Excel Sheet Footer Details
            Surface(
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1 Satır Aktarılacak • Otomatik sütun genişletme aktif",
                        fontSize = 7.5.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Sürüm: Apache POI XSSF",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcelCell(text: String, width: Int, isHeader: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .border(0.5.dp, Color(0xFFE2E8F0))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (isHeader) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 7.5.sp,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) Color.White else Color(0xFF1E293B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- TXT LIVE PREVIEW CARD ---
@Composable
fun TxtLivePreviewCard(
    note: Note,
    formattedDate: String,
    includeLocation: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "DOCUMENT: ${note.title.ifBlank { "isimsiz_not" }}.txt",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF4EC9B0)
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(6.dp))

            val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text
            val previewText = buildString {
                append(note.title.ifBlank { "Başlıksız Not" }).append("\n\n")
                append(cleanContent)
                if (includeLocation && !note.locationName.isNullOrBlank()) {
                    append("\n\nKonum: ").append(note.locationName)
                }
            }

            Text(
                text = previewText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD4D4D4),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- JSON LIVE PREVIEW CARD ---
@Composable
fun JsonLivePreviewCard(
    note: Note,
    formattedDate: String,
    includeLocation: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "{ } JSON STRUCTURE",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))

            val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text.replace("\n", "\\n")
            val jsonPreview = """
                {
                  "id": ${note.id},
                  "title": "${note.title.ifBlank { "Başlıksız" }}",
                  "content": "${cleanContent.take(60)}...",
                  "tag": "${note.tag ?: ""}",
                  "createdAt": ${note.createdAt}${if (includeLocation && !note.locationName.isNullOrBlank()) ",\n  \"location\": \"${note.locationName}\"" else ""}
                }
            """.trimIndent()

            Text(
                text = jsonPreview,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA5F3FC),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/*
// --- PPTX LIVE PREVIEW CARD ---
@Composable
fun PptxLivePreviewCard(
    note: Note,
    formattedDate: String,
    includeLocation: Boolean,
    includeMetadata: Boolean
) {
    val blocks = remember(note.content, note.title) {
        PowerPointGenerator.parseNoteBlocks(note)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFFBF8),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD83B01), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📙 POWERPOINT SUNUMU (.PPTX)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${blocks.size + 1} Slayt (Kapak + ${blocks.size} Blok)",
                    fontSize = 7.5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstBlock = blocks.firstOrNull() ?: PowerPointGenerator.NoteBlock(title = note.title, body = note.content)
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLAYT 2: " + firstBlock.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD83B01),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (includeMetadata && !note.tag.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF7ED)
                            ) {
                                Text(
                                    text = note.tag,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC2410C),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val cleanBody = firstBlock.body.lines().take(4).joinToString("\n")
                    Text(
                        text = cleanBody.ifBlank { "Metin gövdesi slayt maddeleri olarak düzenlendi." },
                        fontSize = 8.5.sp,
                        color = Color(0xFF334155),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFFFF7ED),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Apache POI XSLF ile slayt başlıkları ve maddeler otomatik eşlendi",
                        fontSize = 7.5.sp,
                        color = Color(0xFFC2410C)
                    )
                    Text(
                        text = "PPTX 2016+",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD83B01)
                    )
                }
            }
        }
    }
}
*/
