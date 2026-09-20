package com.lanu.notdefteri.ui.components

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.DocumentShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var exportPreviewFormat by remember { mutableStateOf<ExportFormat?>(null) }

    if (exportPreviewFormat != null) {
        ExportPreviewBottomSheet(
            note = note,
            initialFormat = exportPreviewFormat!!,
            onDismiss = {
                exportPreviewFormat = null
                onDismiss()
            }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier.testTag("share_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Featured Live Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { exportPreviewFormat = ExportFormat.PDF }
                        .testTag("open_live_preview_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Canlı Önizleme",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Canlı Önizleme ile Dışa Aktar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "PDF veya Excel canlı belge tasarımını inceleyin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Diğer Paylaşım Formatları",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                // Sharing Options list
                ShareItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF Önizleme ve Dışa Aktarma",
                    subtitle = "A4 PDF canlı önizlemesi ve özelleştirme",
                    color = Color(0xFFD32F2F),
                    onClick = { exportPreviewFormat = ExportFormat.PDF }
                )

                ShareItem(
                    icon = Icons.Default.TableChart,
                    title = "Excel (.xlsx) Önizleme ve Dışa Aktarma",
                    subtitle = "Tablo canlı önizlemesi ve hücre düzeni",
                    color = Color(0xFF2E7D32),
                    onClick = { exportPreviewFormat = ExportFormat.EXCEL }
                )

                ShareItem(
                    icon = Icons.Default.Share,
                    title = "Düz Metin Olarak Paylaş",
                    subtitle = "Not içeriğini metin olarak gönderin",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        DocumentShareHelper.shareAsPlainText(context, note)
                        onDismiss()
                    }
                )

                ShareItem(
                    icon = Icons.Default.Article,
                    title = "Metin Belgesi (.txt) Olarak Paylaş",
                    subtitle = "Fiziksel .txt dosyası oluşturup paylaşın",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        DocumentShareHelper.shareAsTextFile(context, note)
                        onDismiss()
                    }
                )

                ShareItem(
                    icon = Icons.Default.DataObject,
                    title = "Veri Belgesi (.json) Olarak Paylaş",
                    subtitle = "Yapılandırılmış .json dosyası olarak dışa aktarın",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        DocumentShareHelper.shareAsJsonFile(context, note)
                        onDismiss()
                    }
                )

                ShareItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Panoya Kopyala",
                    subtitle = "Metni cihazınızın hafızasına kopyalayın",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("LANU Not", "${note.title}\n\n${note.content}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Metin panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ShareItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
