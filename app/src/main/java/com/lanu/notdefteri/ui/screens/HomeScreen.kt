package com.lanu.notdefteri.ui.screens

import com.lanu.notdefteri.R
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.util.Date
import java.util.Locale
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.FileProvider
import com.lanu.notdefteri.ui.components.CloudSyncDialog
import com.lanu.notdefteri.ui.components.SyncStatusBadge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.lanu.notdefteri.data.model.ColorPalette
import com.lanu.notdefteri.data.model.DarkModeOption
import com.lanu.notdefteri.data.model.NoteLayoutMode
import com.lanu.notdefteri.data.model.ThemeConfig
import com.lanu.notdefteri.data.model.WallpaperPreset
import com.lanu.notdefteri.ui.components.ThemeCustomizerSheet
import com.lanu.notdefteri.ui.components.AnalyticsSheet
import com.lanu.notdefteri.ui.components.TrashBinSheet
import com.lanu.notdefteri.ui.components.PinLockDialog
import com.lanu.notdefteri.ui.components.PinDialogMode
import com.lanu.notdefteri.ui.components.DrawingCanvasDialog
import com.lanu.notdefteri.ui.components.VoiceMemoPlayer
import com.lanu.notdefteri.ui.components.AiAssistantBottomSheet
import com.lanu.notdefteri.util.AudioRecorderHelper
import com.lanu.notdefteri.util.SecurityManager
import com.lanu.notdefteri.util.LocationHelper
import com.lanu.notdefteri.util.TextRecognitionHelper
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.NoteWithTags
import com.lanu.notdefteri.data.model.Tag
import com.lanu.notdefteri.ui.components.WaveformIndicator
import com.lanu.notdefteri.ui.components.ShareBottomSheet
import com.lanu.notdefteri.ui.components.ReminderPickerDialog
import com.lanu.notdefteri.ui.viewmodel.NoteViewModel
import com.lanu.notdefteri.util.VoiceCommandAction
import com.lanu.notdefteri.util.RichTextHelper
import com.lanu.notdefteri.util.DocumentShareHelper
import com.lanu.notdefteri.util.ReminderScheduler
import com.lanu.notdefteri.util.ColorContrastHelper
import com.lanu.notdefteri.util.ColorUtils
import android.os.Build
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.notesState.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val voiceState by viewModel.parser.state.collectAsStateWithLifecycle()
    val trashNotes by viewModel.trashNotesState.collectAsStateWithLifecycle()
    val trashCount by viewModel.trashCountState.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFoldersState.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteWithTags?>(null) }
    var lockedNoteToUnlock by remember { mutableStateOf<NoteWithTags?>(null) }
    var showSetMasterPinDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderInput by remember { mutableStateOf("") }
    var quickVoiceNoteActive by remember { mutableStateOf(false) }
    var sharingNote by remember { mutableStateOf<Note?>(null) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var reminderNoteTarget by remember { mutableStateOf<Note?>(null) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ML Kit OCR state for Quick Scan Note Creation
    var showOcrSourceDialog by remember { mutableStateOf(false) }
    var ocrInitialText by remember { mutableStateOf<String?>(null) }
    var ocrInitialImagePath by remember { mutableStateOf<String?>(null) }
    var mainTempPhotoFile by remember { mutableStateOf<File?>(null) }

    val mainGalleryOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { selectedUri ->
                Toast.makeText(context, "Görseldeki metin taranıyor...", Toast.LENGTH_SHORT).show()
                TextRecognitionHelper.extractTextFromImage(
                    context = context,
                    imagePathOrUri = selectedUri.toString(),
                    onSuccess = { recognizedText ->
                        ocrInitialText = recognizedText
                        ocrInitialImagePath = selectedUri.toString()
                        editingNote = null
                        showEditor = true
                        Toast.makeText(context, "Görseldeki metin başarıyla taranıp nota aktarıldı!", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )

    val mainCameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val file = mainTempPhotoFile
            if (success || (file != null && file.exists() && file.length() > 0)) {
                file?.let {
                    Toast.makeText(context, "Fotoğraftaki metin taranıyor...", Toast.LENGTH_SHORT).show()
                    TextRecognitionHelper.extractTextFromImage(
                        context = context,
                        imagePathOrUri = file.absolutePath,
                        onSuccess = { recognizedText ->
                            ocrInitialText = recognizedText
                            ocrInitialImagePath = file.absolutePath
                            editingNote = null
                            showEditor = true
                            Toast.makeText(context, "Görseldeki metin başarıyla taranıp nota aktarıldı!", Toast.LENGTH_LONG).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(context, "Kamera çekimi tamamlanamadı. Galeriden görsel seçebilir veya örnek belgeyi deneyebilirsiniz.", Toast.LENGTH_LONG).show()
            }
        }
    )

    val mainCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                try {
                    val file = File(context.cacheDir, "LANU_OCR_${System.currentTimeMillis()}.jpg")
                    mainTempPhotoFile = file
                    file.parentFile?.mkdirs()
                    if (!file.exists()) file.createNewFile()
                    val uri = FileProvider.getUriForFile(context, "com.lanu.notdefteri.fileprovider", file)
                    mainCameraOcrLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Kamera başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Kamera izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val syncState by viewModel.syncManager.state.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    var showThemeSheet by remember { mutableStateOf(false) }

    val syncInfiniteTransition = rememberInfiniteTransition(label = "SyncInfiniteRotation")
    val syncRotationAngle by syncInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SyncRotationAngle"
    )

    // Tag categories collected from DB
    val tags by viewModel.tagsState.collectAsStateWithLifecycle()

    val recentNotes = remember(notes) {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        notes.filter { item -> item.note.createdAt >= twentyFourHoursAgo }
            .sortedByDescending { it.note.createdAt }
    }

    // Speech permissions
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasRecordPermission = granted
            if (!granted) {
                Toast.makeText(context, "Ses kaydı izni reddedildi.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val mainNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Hatırlatıcı bildirimleri için bildirim izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val widgetScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        val intent = activity?.intent
        if (intent != null) {
            val launchNoteId = intent.getIntExtra("LAUNCH_NOTE_ID", -1)
            if (launchNoteId != -1) {
                widgetScope.launch {
                    val note = viewModel.getNoteById(launchNoteId)
                    if (note != null) {
                        editingNote = note
                        showEditor = true
                    }
                }
            } else {
                val action = intent.getStringExtra("LAUNCH_ACTION")
                if (action == "QUICK_RECORD") {
                    quickVoiceNoteActive = true
                }
            }
        }
    }

    // Global voice command handling (e.g., saying "Yeni not" / "Not oluştur" creates a new note)
    LaunchedEffect(voiceState.triggeredAction) {
        if (voiceState.triggeredAction == VoiceCommandAction.NEW_NOTE) {
            viewModel.parser.clearAction()
            editingNote = null
            showEditor = true
            Toast.makeText(context, "Yeni not ekranı açıldı!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Wallpaper layer if active
        if (themeConfig.wallpaper != WallpaperPreset.NONE) {
            val bgDrawableRes = themeConfig.wallpaper.drawableRes
            if (bgDrawableRes != null) {
                Image(
                    painter = painterResource(id = bgDrawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (themeConfig.wallpaper == WallpaperPreset.CUSTOM_GALLERY && themeConfig.customWallpaperUri != null) {
                AsyncImage(
                    model = themeConfig.customWallpaperUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Adaptive Readability Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = themeConfig.wallpaperDim))
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = if (themeConfig.wallpaper != WallpaperPreset.NONE) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .background(
                            if (themeConfig.wallpaper != WallpaperPreset.NONE)
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.background
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                // Header Title & Clean Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_logo_lanu),
                            contentDescription = "LANU Notlar Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "LANU Notlar",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sesli Not Defteri",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontSize = 11.sp
                                    )
                                )
                                SyncStatusBadge(
                                    syncState = syncState,
                                    onClick = { showCloudSyncDialog = true },
                                    isCompact = true
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showHeaderMoreMenu by remember { mutableStateOf(false) }

                        // Theme & Wallpaper Settings Button (Primary Action)
                        IconButton(
                            onClick = { showThemeSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), CircleShape)
                                .testTag("theme_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Tema & Duvar Kağıdı",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More Actions Menu
                        Box {
                            IconButton(
                                onClick = { showHeaderMoreMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                                    .testTag("header_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Diğer Seçenekler",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showHeaderMoreMenu,
                                onDismissRequest = { showHeaderMoreMenu = false },
                                modifier = Modifier.widthIn(min = 200.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Analiz & İstatistikler") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        showAnalyticsSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (themeConfig.layoutMode == NoteLayoutMode.STAGGERED_GRID) 
                                                "Liste Görünümüne Geç" 
                                            else "Izgara Görünümüne Geç"
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (themeConfig.layoutMode == NoteLayoutMode.STAGGERED_GRID) 
                                                Icons.Default.ViewAgenda 
                                            else Icons.Default.Dashboard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        val nextMode = if (themeConfig.layoutMode == NoteLayoutMode.STAGGERED_GRID) {
                                            NoteLayoutMode.SINGLE_COLUMN
                                        } else {
                                            NoteLayoutMode.STAGGERED_GRID
                                        }
                                        viewModel.setLayoutMode(nextMode)
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Bulut Senkronizasyonu", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = if (syncState.isSyncing) "Eşitleniyor..." else if (syncState.lastSyncTime != null) "Son eşitleme hazır" else "Firebase Firestore",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (syncState.isSyncing) Icons.Default.Sync else if (syncState.lastSyncTime != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        showCloudSyncDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Çöp Kutusu")
                                            if (trashCount > 0) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                ) {
                                                    Text("$trashCount", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        showTrashSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Görselden Metin Tara (OCR)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DocumentScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        showOcrSourceDialog = true
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("Tümünü PDF Yap (.pdf)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        if (notes.isNotEmpty()) {
                                            DocumentShareHelper.sharePdfFile(context, notes.map { it.note })
                                        } else {
                                            Toast.makeText(context, "Dışa aktarılacak not bulunmamaktadır.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Excel'e Aktar (.xlsx)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.GridOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        if (notes.isNotEmpty()) {
                                            DocumentShareHelper.shareExcelFile(context, notes.map { it.note })
                                        } else {
                                            Toast.makeText(context, "Dışa aktarılacak not bulunmamaktadır.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kilit & Güvenlik PIN'i") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showHeaderMoreMenu = false
                                        showSetMasterPinDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Animated Sync Status Banner & Progress Bar when pushing/pulling Firestore notes
                AnimatedVisibility(
                    visible = syncState.isSyncing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("sync_progress_indicator_banner")
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .rotate(syncRotationAngle)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncState.statusMessage ?: "Firebase Firestore ile notlar eşitleniyor...",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Modern Search Field with Integrated Quick Mic
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Notlarında ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (!hasRecordPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        quickVoiceNoteActive = true
                                    }
                                },
                                modifier = Modifier.testTag("quick_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Hızlı Sesli Not",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Folder filtering tabs row
                val displayedFolders = remember(allFolders) {
                    (listOf("Kişisel", "İş", "Fikirler", "Eğitim") + allFolders).distinct()
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolder == null,
                            onClick = { viewModel.setSelectedFolder(null) },
                            label = { Text("Tüm Notlar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    items(displayedFolders) { folderName ->
                        FilterChip(
                            selected = selectedFolder == folderName,
                            onClick = {
                                viewModel.setSelectedFolder(if (selectedFolder == folderName) null else folderName)
                            },
                            label = { Text(folderName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedFolder == folderName) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { showNewFolderDialog = true },
                            label = { Text("Klasör Ekle", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "Yeni Klasör", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tag filtering chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedTags.isEmpty(),
                            onClick = { viewModel.clearTagFilters() },
                            label = { Text("Tümü") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(tags) { tag ->
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        FilterChip(
                            selected = selectedTags.contains(tag.name),
                            onClick = { viewModel.toggleTagFilter(tag.name) },
                            label = { 
                                Text(
                                    text = tag.name,
                                    modifier = Modifier.pointerInput(tag) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showDeleteDialog = true
                                            },
                                            onTap = {
                                                viewModel.toggleTagFilter(tag.name)
                                            }
                                        )
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Etiketi Sil") },
                                text = { Text("\"${tag.name}\" etiketini silmek istediğinize emin misiniz? Bu etiketle ilişkilendirilmiş notlar silinmeyecektir.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deleteTag(tag)
                                            showDeleteDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Sil", color = MaterialTheme.colorScheme.onError)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Vazgeç")
                                    }
                                }
                            )
                        }
                    }
                    item {
                        var showAddTagDialog by remember { mutableStateOf(false) }
                        var newTagName by remember { mutableStateOf("") }
                        
                        FilterChip(
                            selected = false,
                            onClick = { showAddTagDialog = true },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = "Yeni Etiket", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ekle")
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        
                        if (showAddTagDialog) {
                            AlertDialog(
                                onDismissRequest = { showAddTagDialog = false },
                                title = { Text("Yeni Etiket Ekle") },
                                text = {
                                    OutlinedTextField(
                                        value = newTagName,
                                        onValueChange = { newTagName = it },
                                        placeholder = { Text("Etiket adı...") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (newTagName.isNotBlank()) {
                                                viewModel.addTag(newTagName)
                                                newTagName = ""
                                                showAddTagDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Ekle")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddTagDialog = false }) {
                                        Text("İptal")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Voice Note FAB (Compact circular FAB)
                SmallFloatingActionButton(
                    onClick = {
                        if (!hasRecordPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            quickVoiceNoteActive = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Sesli Not Oluştur",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick OCR Text Recognition FAB
                SmallFloatingActionButton(
                    onClick = { showOcrSourceDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ocr_scan_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Görselden Metin Tara (OCR)",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Standard New Note FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        editingNote = null
                        showEditor = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Yeni Not") },
                    text = { Text("Yeni Not", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("add_note_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (notes.isEmpty()) {
                // Polished Empty State with Friendly Illustration fallback
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotBlank() || selectedTags.isNotEmpty()) Icons.Default.FilterListOff else Icons.Default.SpeakerNotes,
                        contentDescription = "Yazılı Not Yok",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedTags.isNotEmpty()) "Aramanızla eşleşen not bulunamadı" else "Henüz hiç not eklemediniz",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.testTag("empty_state_text")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sağ üstteki mikrofonu kullanarak anında hızlı sesli notlar oluşturun ya da yeni bir not yazmaya başlayın.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                val isSingleColumn = themeConfig.layoutMode == NoteLayoutMode.SINGLE_COLUMN
                Column(modifier = Modifier.fillMaxSize()) {
                    if (searchQuery.isBlank() && selectedTags.isEmpty() && recentNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Son 24 Saat",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${recentNotes.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recent_notes_row")
                            ) {
                                items(recentNotes) { item ->
                                    RecentNoteCard(
                                        item = item,
                                        hasWallpaper = themeConfig.wallpaper != WallpaperPreset.NONE,
                                        onClick = {
                                            if (item.note.isLocked) {
                                                if (!SecurityManager.isPinSet(context)) {
                                                    showSetMasterPinDialog = true
                                                } else {
                                                    lockedNoteToUnlock = item
                                                }
                                            } else {
                                                editingNote = item
                                                showEditor = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(if (isSingleColumn) 1 else 2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp
                    ) {
                        items(notes) { item ->
                            val note = item.note
                            NoteCard(
                                item = item,
                                hasWallpaper = themeConfig.wallpaper != WallpaperPreset.NONE,
                                isSingleColumn = isSingleColumn,
                                onClick = {
                                    if (note.isLocked) {
                                        if (!SecurityManager.isPinSet(context)) {
                                            showSetMasterPinDialog = true
                                        } else {
                                            lockedNoteToUnlock = item
                                        }
                                    } else {
                                        editingNote = item
                                        showEditor = true
                                    }
                                },
                                onDelete = {
                                    viewModel.moveToTrash(note)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${note.title.ifBlank { "Not" }}\" Çöp Kutusu'na taşındı",
                                            actionLabel = "Geri Al",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreFromTrash(note)
                                        }
                                    }
                                },
                                onTogglePin = { viewModel.togglePin(note) },
                                onShare = { sharingNote = note },
                                onReminderClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        mainNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    reminderNoteTarget = note
                                },
                                onTtsClick = {
                                    val ttsState = viewModel.ttsHelper.state.value
                                    if (ttsState.isSpeaking) {
                                        viewModel.ttsHelper.stop()
                                        Toast.makeText(context, "Seslendirme durduruldu.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val rawText = buildString {
                                            if (note.title.isNotBlank()) append(note.title.trim()).append(". ")
                                            if (note.content.isNotBlank()) append(note.content.trim())
                                        }
                                        val cleanText = RichTextHelper.parseHtmlToAnnotatedString(rawText).text
                                        if (cleanText.isNotBlank()) {
                                            viewModel.ttsHelper.speak(cleanText, "tr-TR", 1.0f)
                                            Toast.makeText(context, "Not seslendiriliyor...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Seslendirilecek metin bulunamadı.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onToggleLock = {
                                    if (!SecurityManager.isPinSet(context)) {
                                        showSetMasterPinDialog = true
                                    } else {
                                        viewModel.toggleNoteLock(note.id, !note.isLocked)
                                        Toast.makeText(context, if (!note.isLocked) "Not kilitlendi" else "Not kilidi kaldırıldı", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Theme Customizer Sheet Overlay
    if (showThemeSheet) {
        ThemeCustomizerSheet(
            themeConfig = themeConfig,
            onDarkModeChange = { viewModel.setDarkMode(it) },
            onPaletteChange = { viewModel.setColorPalette(it) },
            onWallpaperChange = { viewModel.setWallpaper(it) },
            onCustomWallpaperUriChange = { viewModel.setCustomWallpaperUri(it) },
            onWallpaperDimChange = { viewModel.setWallpaperDim(it) },
            onLayoutModeChange = { viewModel.setLayoutMode(it) },
            onDismiss = { showThemeSheet = false }
        )
    }

    // Trash Bin Sheet Overlay
    if (showTrashSheet) {
        TrashBinSheet(
            viewModel = viewModel,
            onDismiss = { showTrashSheet = false }
        )
    }

    // Analytics & Statistics Dashboard Sheet Overlay
    if (showAnalyticsSheet) {
        AnalyticsSheet(
            items = allNotes,
            onDismiss = { showAnalyticsSheet = false }
        )
    }
}

    // Quick OCR Scan Source Selection Dialog
    if (showOcrSourceDialog) {
        AlertDialog(
            onDismissRequest = { showOcrSourceDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Görselden Metin Tara (ML Kit OCR)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El yazısı notların veya basılı belgelerin fotoğrafını çekerek ya da galerinizden seçerek içindeki metinleri anında düzenlenebilir nota dönüştürebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showOcrSourceDialog = false
                                val isPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (isPermissionGranted) {
                                    try {
                                        val file = File(context.cacheDir, "LANU_OCR_${System.currentTimeMillis()}.jpg")
                                        mainTempPhotoFile = file
                                        file.parentFile?.mkdirs()
                                        if (!file.exists()) file.createNewFile()
                                        val uri = FileProvider.getUriForFile(context, "com.lanu.notdefteri.fileprovider", file)
                                        mainCameraOcrLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Kamera başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    mainCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Kameradan Çek & Tara", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Fotoğraf çekip metni otomatik ayıkla", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showOcrSourceDialog = false
                                mainGalleryOcrLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Galeriden Seç & Tara", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Mevcut fotoğraflardan metin çıkar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showOcrSourceDialog = false
                                ocrInitialText = """
📋 TOPLANTI TUTANAĞI & EYLEM PLANI
Tarih: 20 Eylül 2026 - LANU Not Defteri

1. Ses Tanıma & Gerçek Zamanlı Dikte:
   - Dikte motoru, noktalama düzeltmeleri ve sesli komutlar başarıyla test edildi.

2. Görsel Metin Ayıklama (ML Kit OCR):
   - Kamera ve galeriden alınan belgeler yüksek doğrulukla metne dönüştürülmektedir.

3. Senkronizasyon & Veri Güvenliği:
   - Firebase Firestore eşitlemesi ve Excel/PowerPoint aktarımları aktiftir.
""".trimIndent()
                                ocrInitialImagePath = null
                                editingNote = null
                                showEditor = true
                                Toast.makeText(context, "Örnek belge metni başarıyla aktarıldı!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Örnek Belge ile Tara (Önizleme/Demo)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Simülatörde tek dokunuşla hazır metin aktar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOcrSourceDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Full-featured Note Editor Dialog / Overlay
    if (showEditor) {
        NoteEditorDialog(
            item = editingNote,
            onDismiss = {
                showEditor = false
                ocrInitialText = null
                ocrInitialImagePath = null
            },
            onSave = { title, content, tagIds, isPinned, id, imagePath, reminderTime, lat, lng, locName, audioPath, isLocked, folder, drawingPath ->
                viewModel.saveNote(
                    title = title,
                    content = content,
                    id = id,
                    isPinned = isPinned,
                    tagIds = tagIds,
                    imagePath = imagePath,
                    reminderTime = reminderTime,
                    latitude = lat,
                    longitude = lng,
                    locationName = locName,
                    audioPath = audioPath,
                    isLocked = isLocked,
                    folder = folder,
                    drawingPath = drawingPath
                )
                showEditor = false
                ocrInitialText = null
                ocrInitialImagePath = null
            },
            viewModel = viewModel,
            hasRecordPermission = hasRecordPermission,
            requestRecordPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            initialOcrText = ocrInitialText,
            initialOcrImagePath = ocrInitialImagePath,
            onShareNote = { noteToShare ->
                sharingNote = noteToShare
            }
        )
    }

    // Direct Note Card Reminder Dialog
    reminderNoteTarget?.let { targetNote ->
        ReminderPickerDialog(
            noteTitle = targetNote.title,
            currentReminderTime = targetNote.reminderTime,
            onDismiss = { reminderNoteTarget = null },
            onScheduleReminder = { triggerTime ->
                viewModel.scheduleReminder(targetNote, triggerTime)
                reminderNoteTarget = null
                Toast.makeText(context, "Hatırlatıcı kuruldu: ${ReminderScheduler.formatReminderDate(triggerTime)}", Toast.LENGTH_SHORT).show()
            },
            onCancelReminder = {
                viewModel.cancelReminder(targetNote)
                reminderNoteTarget = null
                Toast.makeText(context, "Hatırlatıcı kaldırıldı", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Firebase Firestore Cloud Sync Dialog
    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            localNotesCount = notes.size,
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    // Quick Voice Note Dialog Overlay
    if (quickVoiceNoteActive) {
        QuickVoiceNoteDialog(
            onDismiss = {
                viewModel.parser.stopListening()
                quickVoiceNoteActive = false
            },
            onSave = { title, content, tagIds ->
                viewModel.saveNote(
                    title = title,
                    content = content,
                    tagIds = tagIds
                )
                quickVoiceNoteActive = false
                Toast.makeText(context, "Sesli not başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            hasRecordPermission = hasRecordPermission,
            requestRecordPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            availableTags = tags
        )
    }

    // Share Bottom Sheet Overlay
    sharingNote?.let { noteToShare ->
        ShareBottomSheet(
            note = noteToShare,
            onDismiss = { sharingNote = null }
        )
    }

    // Locked Note PIN Verification Dialog
    lockedNoteToUnlock?.let { lockedItem ->
        val lockedNote = lockedItem.note
        PinLockDialog(
            mode = PinDialogMode.VERIFY,
            title = "\"${lockedNote.title.ifBlank { "Gizli Not" }}\"",
            onSuccess = {
                val n = lockedItem
                lockedNoteToUnlock = null
                editingNote = n
                showEditor = true
            },
            onDismiss = { lockedNoteToUnlock = null }
        )
    }

    // Set Master Security PIN Dialog
    if (showSetMasterPinDialog) {
        PinLockDialog(
            mode = PinDialogMode.SET_NEW_PIN,
            title = if (SecurityManager.isPinSet(context)) "Güvenlik PIN'ini Değiştir" else "4 Haneli Güvenlik PIN'i Belirle",
            onSuccess = {
                showSetMasterPinDialog = false
                Toast.makeText(context, "Güvenlik PIN kodu başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSetMasterPinDialog = false }
        )
    }

    // Create New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false; newFolderInput = "" },
            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Yeni Klasör Oluştur") },
            text = {
                Column {
                    Text(
                        "Notlarınızı tematik klasörler altında gruplayın (örn: Projeler, Seyahat, Finans).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderInput,
                        onValueChange = { newFolderInput = it },
                        label = { Text("Klasör Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = newFolderInput.trim()
                        if (folder.isNotBlank()) {
                            viewModel.setSelectedFolder(folder)
                            showNewFolderDialog = false
                            newFolderInput = ""
                            Toast.makeText(context, "\"$folder\" klasörü seçildi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false; newFolderInput = "" }) {
                    Text("İptal")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    item: NoteWithTags,
    hasWallpaper: Boolean = false,
    isSingleColumn: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onReminderClick: () -> Unit,
    onTtsClick: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val note = item.note
    val dateString = remember(note.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))
        sdf.format(Date(note.createdAt))
    }

    val wordCount = remember(note.content) {
        if (note.content.isBlank()) 0
        else note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    val cardBgColor = if (note.isPinned) {
        if (hasWallpaper) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        if (hasWallpaper) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        else MaterialTheme.colorScheme.surface
    }

    val titleTextColor = ColorUtils.getPrimaryTitleColor(cardBgColor)
    val bodyTextColor = ColorUtils.getBodyContentColor(cardBgColor)
    val subtleTextColor = ColorUtils.getSubtleMetadataColor(cardBgColor)
    val iconTint = ColorUtils.getActionIconTint(cardBgColor)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        border = BorderStroke(
            width = if (note.isPinned) 1.5.dp else 1.dp,
            color = if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasWallpaper) 0.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onTogglePin
            )
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            note.imagePath?.let { path ->
                if (path.isNotBlank()) {
                    AsyncImage(
                        model = path,
                        contentDescription = "Not Fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSingleColumn) 160.dp else 115.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tag badges
                    if (item.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.tags.forEach { tag ->
                                val tagName = tag.name
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (tagName) {
                                                "Kişisel" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                "İş" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                "Yapılacaklar" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tagName,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tagName) {
                                                "Kişisel" -> MaterialTheme.colorScheme.primary
                                                "İş" -> MaterialTheme.colorScheme.secondary
                                                "Yapılacaklar" -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Folder badge
                    note.folder?.let { folderName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Klasör",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Location badge
                    note.locationName?.let { locName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Konum",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = locName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    var showNoteMenu by remember { mutableStateOf(false) }

                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Sabitlendi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli Not",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    val isReminderActive = note.reminderTime != null && note.reminderTime > System.currentTimeMillis()
                    IconButton(
                        onClick = onReminderClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("note_reminder_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (isReminderActive) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Hatırlatıcı Ayarla",
                            tint = if (isReminderActive) MaterialTheme.colorScheme.primary else iconTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onTtsClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("note_tts_speak_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Notu Seslendir",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showNoteMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Seçenekler",
                                tint = iconTint.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showNoteMenu,
                            onDismissRequest = { showNoteMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isLocked) "Kilidi Kaldır" else "Notu Kilitle") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showNoteMenu = false
                                    onToggleLock?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Notu Paylaş") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showNoteMenu = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Notu Sil") },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showNoteMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Note title
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSingleColumn) 18.sp else 15.sp,
                        color = titleTextColor
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Note snippet content or privacy lock banner
            if (note.isLocked) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Gizli",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gizli Not (Açmak için dokunun)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (note.content.isNotBlank()) {
                    Text(
                        text = RichTextHelper.parseHtmlToAnnotatedString(
                            text = note.content,
                            primaryColor = titleTextColor,
                            header2Color = bodyTextColor,
                            header3Color = bodyTextColor.copy(alpha = 0.85f),
                            codeBackgroundColor = bodyTextColor.copy(alpha = 0.1f),
                            codeTextColor = bodyTextColor
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = bodyTextColor
                        ),
                        maxLines = if (isSingleColumn) 6 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!note.drawingPath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AsyncImage(
                        model = note.drawingPath,
                        contentDescription = "El Çizimi",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSingleColumn) 140.dp else 90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )
                }
            }

            // Voice Memo badge on NoteCard if attached
            if (!note.audioPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Ses Kaydı",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ses Kaydı Ekli",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }

            // Reminder Badge on NoteCard if scheduled
            if (note.reminderTime != null) {
                val hasReminder = note.reminderTime > System.currentTimeMillis()
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasReminder) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReminderClick() }
                        .testTag("note_reminder_badge_${note.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasReminder) Icons.Default.Alarm else Icons.Default.AlarmOn,
                            contentDescription = "Hatırlatıcı",
                            tint = if (hasReminder) MaterialTheme.colorScheme.primary else iconTint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (hasReminder) {
                                "${ReminderScheduler.formatReminderDate(note.reminderTime)} (${ReminderScheduler.getRemainingTimeString(note.reminderTime)})"
                            } else {
                                "Hatırlatıldı (${ReminderScheduler.formatReminderDate(note.reminderTime)})"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (hasReminder) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasReminder) MaterialTheme.colorScheme.onPrimaryContainer else bodyTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer metadata: Word count & Date string
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount kelime",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtleTextColor
                    )
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtleTextColor
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    item: NoteWithTags?,
    onDismiss: () -> Unit,
    onSave: (String, String, List<Int>, Boolean, Int, String?, Long?, Double?, Double?, String?, String?, Boolean, String?, String?) -> Unit,
    viewModel: NoteViewModel,
    hasRecordPermission: Boolean,
    requestRecordPermission: () -> Unit,
    initialOcrText: String? = null,
    initialOcrImagePath: String? = null,
    onShareNote: ((Note) -> Unit)? = null
) {
    val note = item?.note
    var title by remember(note?.id) { mutableStateOf(note?.title ?: if (!initialOcrText.isNullOrBlank()) "Görsel Taraması (${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())})" else "") }
    var contentValue by remember(note?.id) { mutableStateOf(TextFieldValue(note?.content ?: (initialOcrText ?: ""))) }
    var selectedTagsIds by remember(note?.id) { mutableStateOf(item?.tags?.map { it.id }?.toSet() ?: emptySet()) }
    var isPinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var reminderTimeState by remember(note?.id) { mutableStateOf(note?.reminderTime) }
    var locationState by remember(note?.id) { mutableStateOf(note?.locationName) }
    var latitudeState by remember(note?.id) { mutableStateOf(note?.latitude) }
    var longitudeState by remember(note?.id) { mutableStateOf(note?.longitude) }
    var activeLangCode by remember { mutableStateOf("tr-TR") }

    // Enhanced Features State
    var isLockedState by remember(note?.id) { mutableStateOf(note?.isLocked ?: false) }
    var folderState by remember(note?.id) { mutableStateOf(note?.folder) }
    var drawingPathState by remember(note?.id) { mutableStateOf(note?.drawingPath) }
    var audioPathState by remember(note?.id) { mutableStateOf(note?.audioPath) }

    var showDrawingCanvasDialog by remember { mutableStateOf(false) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var showFolderSelectionDialog by remember { mutableStateOf(false) }
    var showPinDialogForLock by remember { mutableStateOf(false) }
    
    val ttsState by viewModel.ttsHelper.state.collectAsStateWithLifecycle()
    var showTtsPlayer by remember { mutableStateOf(false) }
    var ttsLanguage by remember { mutableStateOf("tr-TR") }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val audioRecorder = remember { AudioRecorderHelper(context) }
    val isAudioRecording by audioRecorder.isRecording.collectAsStateWithLifecycle()
    val audioRecordingSeconds by audioRecorder.recordingSeconds.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.ttsHelper.stop()
            audioRecorder.release()
        }
    }
    
    var currentNoteId by remember(note?.id) { mutableStateOf(note?.id ?: 0) }
    var saveStatus by remember(note?.id) { mutableStateOf(if (note == null) "Taslak" else "Tüm değişiklikler kaydedildi") }

    LaunchedEffect(initialOcrText) {
        if (!initialOcrText.isNullOrBlank() && currentNoteId == 0) {
            coroutineScope.launch {
                val assignedId = viewModel.saveNoteAndGetId(
                    title = title,
                    content = initialOcrText,
                    id = 0,
                    isPinned = isPinned,
                    tagIds = selectedTagsIds.toList(),
                    imagePath = initialOcrImagePath
                )
                currentNoteId = assignedId
                saveStatus = "Görsel taraması kaydedildi"
            }
        }
    }

    var imagePathState by remember(note?.id) { mutableStateOf(note?.imagePath ?: initialOcrImagePath) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var isOcrScanning by remember { mutableStateOf(false) }
    var isMarkdownPreviewMode by remember { mutableStateOf(false) }
    var dictationTargetTitle by remember { mutableStateOf(false) }
    var showVoiceHelpDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val file = tempPhotoFile
            if (success || (file != null && file.exists() && file.length() > 0)) {
                file?.let {
                    imagePathState = file.absolutePath
                    coroutineScope.launch {
                        if (currentNoteId == 0) {
                            val assignedId = viewModel.saveNoteAndGetId(
                                title = title,
                                content = contentValue.text,
                                id = 0,
                                isPinned = isPinned,
                                tagIds = selectedTagsIds.toList(),
                                imagePath = file.absolutePath
                            )
                            currentNoteId = assignedId
                        } else {
                            viewModel.saveNote(
                                title = title,
                                content = contentValue.text,
                                id = currentNoteId,
                                isPinned = isPinned,
                                tagIds = selectedTagsIds.toList(),
                                imagePath = file.absolutePath
                            )
                        }
                    }
                    isOcrScanning = true
                    Toast.makeText(context, "Kameradan alınan fotoğraf taranıyor...", Toast.LENGTH_SHORT).show()
                    TextRecognitionHelper.extractTextFromImage(
                        context = context,
                        imagePathOrUri = file.absolutePath,
                        onSuccess = { recognizedText ->
                            isOcrScanning = false
                            val currentText = contentValue.text
                            val updated = if (currentText.isBlank()) recognizedText else "$currentText\n\n$recognizedText"
                            contentValue = TextFieldValue(
                                text = updated,
                                selection = TextRange(updated.length)
                            )
                            Toast.makeText(context, "Fotoğraftaki metin taranıp nota eklendi!", Toast.LENGTH_LONG).show()
                        },
                        onError = { errorMsg ->
                            isOcrScanning = false
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(context, "Kamera çekimi tamamlanamadı. Galeriden görsel seçebilirsiniz.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { selectedUri ->
                imagePathState = selectedUri.toString()
                coroutineScope.launch {
                    if (currentNoteId == 0) {
                        val assignedId = viewModel.saveNoteAndGetId(
                            title = title,
                            content = contentValue.text,
                            id = 0,
                            isPinned = isPinned,
                            tagIds = selectedTagsIds.toList(),
                            imagePath = selectedUri.toString()
                        )
                        currentNoteId = assignedId
                    } else {
                        viewModel.saveNote(
                            title = title,
                            content = contentValue.text,
                            id = currentNoteId,
                            isPinned = isPinned,
                            tagIds = selectedTagsIds.toList(),
                            imagePath = selectedUri.toString()
                        )
                    }
                }
                isOcrScanning = true
                Toast.makeText(context, "Galerideki görsel taranıyor...", Toast.LENGTH_SHORT).show()
                TextRecognitionHelper.extractTextFromImage(
                    context = context,
                    imagePathOrUri = selectedUri.toString(),
                    onSuccess = { recognizedText ->
                        isOcrScanning = false
                        val currentText = contentValue.text
                        val updated = if (currentText.isBlank()) recognizedText else "$currentText\n\n$recognizedText"
                        contentValue = TextFieldValue(
                            text = updated,
                            selection = TextRange(updated.length)
                        )
                        Toast.makeText(context, "Görseldeki metin taranıp nota eklendi!", Toast.LENGTH_LONG).show()
                    },
                    onError = { errorMsg ->
                        isOcrScanning = false
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                try {
                    val file = File(context.cacheDir, "LANU_CAP_${System.currentTimeMillis()}.jpg")
                    tempPhotoFile = file
                    file.parentFile?.mkdirs()
                    if (!file.exists()) file.createNewFile()
                    val uri = FileProvider.getUriForFile(context, "com.lanu.notdefteri.fileprovider", file)
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Kamera başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Kamera izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    var showReminderDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                showReminderDialog = true
            } else {
                Toast.makeText(context, "Hatırlatıcılar için bildirim izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions.values.any { it }
            if (isGranted) {
                Toast.makeText(context, "Mevcut konum alınıyor...", Toast.LENGTH_SHORT).show()
                LocationHelper.getCurrentLocation(
                    context = context,
                    onSuccess = { loc ->
                        latitudeState = loc.latitude
                        longitudeState = loc.longitude
                        locationState = loc.addressName
                        Toast.makeText(context, "Konum eklendi: ${loc.addressName}", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(context, "Konum hatırlatıcısı için konum izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                val updated = viewModel.parser.appendTranscription(contentValue.text, text, activeLangCode)
                contentValue = TextFieldValue(
                    text = updated,
                    selection = androidx.compose.ui.text.TextRange(updated.length)
                )
            }
        }
    }

    val voiceState by viewModel.parser.state.collectAsStateWithLifecycle()

    // Periodical Auto-Save Mechanism
    LaunchedEffect(title, contentValue.text, selectedTagsIds, isPinned, imagePathState, locationState, latitudeState, longitudeState, isLockedState, folderState, drawingPathState, audioPathState) {
        // Skip initial run if nothing changed from initial note values to avoid redundant database writes
        if (title == (note?.title ?: "") && 
            contentValue.text == (note?.content ?: "") && 
            selectedTagsIds == (item?.tags?.map { it.id }?.toSet() ?: emptySet<Int>()) && 
            isPinned == (note?.isPinned ?: false) &&
            imagePathState == note?.imagePath &&
            locationState == note?.locationName &&
            isLockedState == (note?.isLocked ?: false) &&
            folderState == note?.folder &&
            drawingPathState == note?.drawingPath &&
            audioPathState == note?.audioPath &&
            currentNoteId == (note?.id ?: 0)
        ) {
            return@LaunchedEffect
        }

        // Only auto-save if there's actual content or title or image or drawing or audio to save
        if (title.isBlank() && contentValue.text.isBlank() && (imagePathState ?: "").isBlank() && (drawingPathState ?: "").isBlank() && (audioPathState ?: "").isBlank()) {
            return@LaunchedEffect
        }

        delay(1500) // Debounce for 1.5 seconds
        saveStatus = "Kaydediliyor..."
        
        try {
            val assignedId = viewModel.saveNoteAndGetId(
                title = title,
                content = contentValue.text,
                id = currentNoteId,
                isPinned = isPinned,
                tagIds = selectedTagsIds.toList(),
                imagePath = imagePathState,
                reminderTime = reminderTimeState,
                latitude = latitudeState,
                longitude = longitudeState,
                locationName = locationState,
                audioPath = audioPathState,
                isLocked = isLockedState,
                folder = folderState,
                drawingPath = drawingPathState
            )
            currentNoteId = assignedId
            saveStatus = "Tüm değişiklikler kaydedildi"
        } catch (e: Exception) {
            saveStatus = "Otomatik kaydetme başarısız"
        }
    }

    // Handle incoming voice recognition text appending with smart formatting & punctuation
    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            if (dictationTargetTitle) {
                val updated = viewModel.parser.appendTranscription(title, voiceState.spokenText, activeLangCode)
                title = updated
            } else {
                val updated = viewModel.parser.appendTranscription(contentValue.text, voiceState.spokenText, activeLangCode)
                contentValue = TextFieldValue(
                    text = updated,
                    selection = androidx.compose.ui.text.TextRange(updated.length)
                )
            }
            viewModel.parser.clearSpokenText()
        }
    }

    // Handle hands-free voice commands in NoteEditorDialog (e.g. "Notu kaydet" or "Temizle")
    LaunchedEffect(voiceState.triggeredAction) {
        when (voiceState.triggeredAction) {
            VoiceCommandAction.SAVE -> {
                viewModel.parser.clearAction()
                viewModel.parser.stopListening()
                if (title.isNotBlank() || contentValue.text.isNotBlank() || imagePathState != null || drawingPathState != null || audioPathState != null) {
                    onSave(
                        title.ifBlank { "Başlıksız Not" },
                        contentValue.text,
                        selectedTagsIds.toList(),
                        isPinned,
                        currentNoteId,
                        imagePathState,
                        reminderTimeState,
                        latitudeState,
                        longitudeState,
                        locationState,
                        audioPathState,
                        isLockedState,
                        folderState,
                        drawingPathState
                    )
                }
            }
            VoiceCommandAction.CLEAR -> {
                viewModel.parser.clearAction()
                contentValue = TextFieldValue("")
            }
            VoiceCommandAction.PIN -> {
                viewModel.parser.clearAction()
                isPinned = !isPinned
                Toast.makeText(context, if (isPinned) "Not sabitlendi" else "Sabitleme kaldırıldı", Toast.LENGTH_SHORT).show()
            }
            VoiceCommandAction.TITLE -> {
                val newTitle = voiceState.commandPayload ?: ""
                viewModel.parser.clearAction()
                if (newTitle.isNotBlank()) {
                    title = newTitle
                    Toast.makeText(context, "Başlık ayarlandı: \"$newTitle\"", Toast.LENGTH_SHORT).show()
                }
            }
            VoiceCommandAction.TAG -> {
                val newTag = voiceState.commandPayload ?: ""
                viewModel.parser.clearAction()
                if (newTag.isNotBlank()) {
                    coroutineScope.launch {
                        val tagId = viewModel.addTagAndGetId(newTag)
                        if (tagId != -1) {
                            selectedTagsIds = selectedTagsIds + tagId
                            Toast.makeText(context, "Etiket eklendi: \"$newTag\"", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.ttsHelper.stop()
            viewModel.parser.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Custom Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.ttsHelper.stop()
                        viewModel.parser.stopListening()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (note == null) "Yeni Not Oluştur" else "Notu Düzenle",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = saveStatus,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = when (saveStatus) {
                                    "Kaydediliyor..." -> MaterialTheme.colorScheme.primary
                                    "Tüm değişiklikler kaydedildi" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    "Otomatik kaydetme başarısız" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        var showEditorToolsMenu by remember { mutableStateOf(false) }

                        // AI Assistant Button
                        IconButton(
                            onClick = { showAiBottomSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("note_ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Yapay Zeka Asistanı",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Voice Dictation Toggle (Speech to Text)
                        IconButton(
                            onClick = {
                                if (!hasRecordPermission) {
                                    requestRecordPermission()
                                } else {
                                    if (voiceState.isListening) {
                                        viewModel.parser.stopListening()
                                    } else {
                                        viewModel.parser.startListening(activeLangCode, continuous = true)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (voiceState.isListening) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("dictate_top_button")
                        ) {
                            Icon(
                                imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.KeyboardVoice,
                                contentDescription = if (voiceState.isListening) "Dikteyi Durdur" else "Mikrofonla Yazdır",
                                tint = if (voiceState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Toggle Pin Button
                        IconButton(
                            onClick = { isPinned = !isPinned },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Sabitle",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Additional Tools Menu
                        Box {
                            val hasActiveReminder = reminderTimeState != null && reminderTimeState!! > System.currentTimeMillis()
                            val hasLocation = locationState != null

                            IconButton(
                                onClick = { showEditorToolsMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("editor_tools_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Not Araçları",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showEditorToolsMenu,
                                onDismissRequest = { showEditorToolsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Klasör Seç (${folderState ?: "Genel"})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        showFolderSelectionDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("El Çizimi / Karalama Ekle") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        showDrawingCanvasDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (isAudioRecording) "Ses Kaydını Durdur" else "Ses Kaydı Ekle") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isAudioRecording) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = if (isAudioRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        if (!hasRecordPermission) {
                                            requestRecordPermission()
                                        } else {
                                            if (isAudioRecording) {
                                                val savedFile = audioRecorder.stopRecording()
                                                if (savedFile != null) {
                                                    audioPathState = savedFile
                                                    Toast.makeText(context, "Ses kaydı nota eklendi!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                audioRecorder.startRecording()
                                                Toast.makeText(context, "Ses kaydı başlatıldı...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (isLockedState) "Not Kilidini Kaldır" else "Notu Güvenli Kilitle") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isLockedState) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        if (!isLockedState && !SecurityManager.isPinSet(context)) {
                                            showPinDialogForLock = true
                                        } else {
                                            isLockedState = !isLockedState
                                            Toast.makeText(context, if (isLockedState) "Not kilitlendi" else "Not kilidi kaldırıldı", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kameradan Çek & Tara (OCR)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        val isPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (isPermissionGranted) {
                                            try {
                                                val file = File(context.cacheDir, "LANU_CAP_${System.currentTimeMillis()}.jpg")
                                                tempPhotoFile = file
                                                file.parentFile?.mkdirs()
                                                if (!file.exists()) file.createNewFile()
                                                val uri = FileProvider.getUriForFile(context, "com.lanu.notdefteri.fileprovider", file)
                                                cameraLauncher.launch(uri)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Kamera başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Galeriden Seç & Tara (OCR)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        galleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (hasActiveReminder) "Hatırlatıcı Aktif" else "Hatırlatıcı Ayarla") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (hasActiveReminder) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                                            contentDescription = null,
                                            tint = if (hasActiveReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            showReminderDialog = true
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (hasLocation) "Konum: $locationState" else "Konum Ekle") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (hasLocation) Icons.Default.LocationOn else Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = if (hasLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (fineGranted || coarseGranted) {
                                            Toast.makeText(context, "Mevcut konum alınıyor...", Toast.LENGTH_SHORT).show()
                                            LocationHelper.getCurrentLocation(
                                                context = context,
                                                onSuccess = { loc ->
                                                    latitudeState = loc.latitude
                                                    longitudeState = loc.longitude
                                                    locationState = loc.addressName
                                                    Toast.makeText(context, "Konum eklendi: ${loc.addressName}", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (ttsState.isSpeaking) "Seslendirmeyi Durdur" else "Notu Sesli Dinle (TTS)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (ttsState.isSpeaking) Icons.Default.VolumeUp else Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = if (ttsState.isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        val fullTextToSpeak = buildString {
                                            if (title.isNotBlank()) append(title.trim()).append(". ")
                                            if (contentValue.text.isNotBlank()) append(contentValue.text.trim())
                                        }
                                        if (fullTextToSpeak.isBlank()) {
                                            Toast.makeText(context, "Seslendirilecek metin bulunamadı.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showTtsPlayer = true
                                            if (ttsState.isSpeaking) {
                                                viewModel.ttsHelper.stop()
                                            } else {
                                                viewModel.ttsHelper.speak(fullTextToSpeak, ttsLanguage, ttsSpeed)
                                            }
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Paylaş & Dışa Aktar (PDF, Excel...)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showEditorToolsMenu = false
                                        val noteToShare = Note(
                                            id = currentNoteId,
                                            title = title.ifBlank { "İsimsiz Not" },
                                            content = contentValue.text,
                                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                                            isPinned = isPinned,
                                            tag = null,
                                            imagePath = imagePathState,
                                            reminderTime = reminderTimeState,
                                            latitude = latitudeState,
                                            longitude = longitudeState,
                                            locationName = locationState,
                                            audioPath = audioPathState,
                                            isLocked = isLockedState,
                                            folder = folderState,
                                            drawingPath = drawingPathState
                                        )
                                        onShareNote?.invoke(noteToShare)
                                    }
                                )
                            }
                        }

                        // Save Checkmark Button
                        IconButton(
                            onClick = {
                                viewModel.parser.stopListening()
                                viewModel.ttsHelper.stop()
                                onSave(
                                    title,
                                    contentValue.text,
                                    selectedTagsIds.toList(),
                                    isPinned,
                                    currentNoteId,
                                    imagePathState,
                                    reminderTimeState,
                                    latitudeState,
                                    longitudeState,
                                    locationState,
                                    audioPathState,
                                    isLockedState,
                                    folderState,
                                    drawingPathState
                                )
                            },
                            enabled = contentValue.text.isNotBlank() || title.isNotBlank() || !drawingPathState.isNullOrBlank() || !audioPathState.isNullOrBlank(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Kaydet",
                                tint = if (contentValue.text.isNotBlank() || title.isNotBlank() || !drawingPathState.isNullOrBlank() || !audioPathState.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (showReminderDialog) {
                    ReminderPickerDialog(
                        noteTitle = title.ifBlank { "Yeni Not" },
                        currentReminderTime = reminderTimeState,
                        onDismiss = { showReminderDialog = false },
                        onScheduleReminder = { triggerTime ->
                            reminderTimeState = triggerTime
                            showReminderDialog = false
                            coroutineScope.launch {
                                if (currentNoteId == 0) {
                                    val assignedId = viewModel.saveNoteAndGetId(
                                        title = title,
                                        content = contentValue.text,
                                        id = 0,
                                        isPinned = isPinned,
                                        tagIds = selectedTagsIds.toList(),
                                        imagePath = imagePathState,
                                        reminderTime = triggerTime
                                    )
                                    currentNoteId = assignedId
                                } else {
                                    viewModel.saveNote(
                                        title = title,
                                        content = contentValue.text,
                                        id = currentNoteId,
                                        isPinned = isPinned,
                                        tagIds = selectedTagsIds.toList(),
                                        imagePath = imagePathState,
                                        reminderTime = triggerTime
                                    )
                                }
                                Toast.makeText(context, "Hatırlatıcı kuruldu: ${ReminderScheduler.formatReminderDate(triggerTime)}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelReminder = {
                            reminderTimeState = null
                            showReminderDialog = false
                            if (currentNoteId != 0) {
                                ReminderScheduler.cancelReminder(context, currentNoteId)
                                coroutineScope.launch {
                                    viewModel.saveNote(
                                        title = title,
                                        content = contentValue.text,
                                        id = currentNoteId,
                                        isPinned = isPinned,
                                        tagIds = selectedTagsIds.toList(),
                                        imagePath = imagePathState,
                                        reminderTime = null
                                    )
                                }
                            }
                            Toast.makeText(context, "Hatırlatıcı iptal edildi.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Tag selector chip row
                    Text(
                        text = "Kategoriler:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val categories by viewModel.tagsState.collectAsStateWithLifecycle()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedTagsIds.contains(cat.id),
                                onClick = { 
                                    selectedTagsIds = if (selectedTagsIds.contains(cat.id)) {
                                        selectedTagsIds - cat.id
                                    } else {
                                        selectedTagsIds + cat.id
                                    }
                                },
                                label = { Text(cat.name) },
                                leadingIcon = if (selectedTagsIds.contains(cat.id)) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null
                            )
                        }
                        item {
                            var showAddTagDialog by remember { mutableStateOf(false) }
                            var newTagName by remember { mutableStateOf("") }
                            
                            AssistChip(
                                onClick = { showAddTagDialog = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = "Yeni Kategori", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Yeni Ekle")
                                    }
                                }
                            )
                            
                            if (showAddTagDialog) {
                                AlertDialog(
                                    onDismissRequest = { showAddTagDialog = false },
                                    title = { Text("Yeni Kategori Ekle") },
                                    text = {
                                        OutlinedTextField(
                                            value = newTagName,
                                            onValueChange = { newTagName = it },
                                            placeholder = { Text("Kategori adı...") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (newTagName.isNotBlank()) {
                                                    coroutineScope.launch {
                                                        val tagId = viewModel.addTagAndGetId(newTagName)
                                                        if (tagId != -1) {
                                                            selectedTagsIds = selectedTagsIds + tagId
                                                        }
                                                        newTagName = ""
                                                        showAddTagDialog = false
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Ekle")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showAddTagDialog = false }) {
                                            Text("İptal")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Folder and Lock status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFolderSelectionDialog = true }
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Klasör: ${folderState ?: "Genel"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (isLockedState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Gizli Not (Kilitli)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Active Audio Recording Banner
                    if (isAudioRecording) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ses Kaydediliyor... ${String.format(Locale.getDefault(), "%02d:%02d", audioRecordingSeconds / 60, audioRecordingSeconds % 60)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                                Button(
                                    onClick = {
                                        val savedFile = audioRecorder.stopRecording()
                                        if (savedFile != null) {
                                            audioPathState = savedFile
                                            Toast.makeText(context, "Ses kaydı nota eklendi!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Kaydı Bitir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }

                    // Voice Memo Player if attached
                    if (!audioPathState.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        VoiceMemoPlayer(
                            audioPath = audioPathState!!,
                            onDelete = {
                                audioPathState = null
                                Toast.makeText(context, "Ses kaydı kaldırıldı", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Drawing Preview Card if attached
                    if (!drawingPathState.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Draw,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "El Çizimi / Karalama",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = { showDrawingCanvasDialog = true }) {
                                            Text("Düzenle", fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { drawingPathState = null }) {
                                            Text("Kaldır", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                AsyncImage(
                                    model = drawingPathState,
                                    contentDescription = "El Çizimi",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Active reminder pill in editor
                    if (reminderTimeState != null && reminderTimeState!! > System.currentTimeMillis()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showReminderDialog = true }
                                .testTag("editor_active_reminder_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Hatırlatıcı",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hatırlatıcı: ${ReminderScheduler.formatReminderDate(reminderTimeState!!)} (${ReminderScheduler.getRemainingTimeString(reminderTimeState!!)})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        reminderTimeState = null
                                        if (currentNoteId != 0) {
                                            ReminderScheduler.cancelReminder(context, currentNoteId)
                                            coroutineScope.launch {
                                                viewModel.saveNote(
                                                    title = title,
                                                    content = contentValue.text,
                                                    id = currentNoteId,
                                                    isPinned = isPinned,
                                                    tagIds = selectedTagsIds.toList(),
                                                    imagePath = imagePathState,
                                                    reminderTime = null
                                                )
                                            }
                                        }
                                        Toast.makeText(context, "Hatırlatıcı kaldırıldı", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kaldır",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active location pill in editor
                    locationState?.let { locName ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("editor_active_location_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Konum",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📍 Konum: $locName",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        locationState = null
                                        latitudeState = null
                                        longitudeState = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Konumu Kaldır",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    imagePathState?.let { path ->
                        if (path.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("attached_image_ocr_card")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    ) {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Not Fotoğrafı",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { imagePathState = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Fotoğrafı Kaldır",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // ML Kit Text Recognition Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DocumentScanner,
                                                contentDescription = "ML Kit OCR",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Google ML Kit Metin Taraması",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = "Görseldeki yazıları nota otomatik ekleyin",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isOcrScanning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Button(
                                                onClick = {
                                                    isOcrScanning = true
                                                    Toast.makeText(context, "Görseldeki metin taranıyor...", Toast.LENGTH_SHORT).show()
                                                    TextRecognitionHelper.extractTextFromImage(
                                                        context = context,
                                                        imagePathOrUri = path,
                                                        onSuccess = { recognizedText ->
                                                            isOcrScanning = false
                                                            val currentText = contentValue.text
                                                            val updated = if (currentText.isBlank()) recognizedText else "$currentText\n\n$recognizedText"
                                                            contentValue = TextFieldValue(
                                                                text = updated,
                                                                selection = androidx.compose.ui.text.TextRange(updated.length)
                                                            )
                                                            Toast.makeText(context, "Görseldeki metin nota eklendi!", Toast.LENGTH_LONG).show()
                                                        },
                                                        onError = { errorMsg ->
                                                            isOcrScanning = false
                                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("extract_text_from_image_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoFixHigh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Metni Tara",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Animated Text-to-Speech (TTS) Player Bar
                    AnimatedVisibility(
                        visible = showTtsPlayer || ttsState.isSpeaking,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("tts_player_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (ttsState.isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (ttsState.isSpeaking) Icons.Default.VolumeUp else Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = if (ttsState.isSpeaking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = if (ttsState.isSpeaking) "Not Seslendiriliyor..." else "Sesli Okuma (TTS)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (ttsState.isSpeaking) "Metin okunuyor, dinleyin" else "Notu sesli dinlemek için başlatın",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.ttsHelper.stop()
                                            showTtsPlayer = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (ttsState.isSpeaking) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    WaveformIndicator(
                                        isListening = true,
                                        rmsDb = 6.0f,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    )
                                }

                                ttsState.error?.let { err ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = err,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play / Stop Main Button & Language Toggle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledIconButton(
                                            onClick = {
                                                if (ttsState.isSpeaking) {
                                                    viewModel.ttsHelper.stop()
                                                } else {
                                                    val rawText = buildString {
                                                        if (title.isNotBlank()) append(title.trim()).append(". ")
                                                        if (contentValue.text.isNotBlank()) append(contentValue.text.trim())
                                                    }
                                                    val fullText = RichTextHelper.parseHtmlToAnnotatedString(rawText).text
                                                    if (fullText.isBlank()) {
                                                        Toast.makeText(context, "Seslendirilecek metin bulunamadı.", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        viewModel.ttsHelper.speak(fullText, ttsLanguage, ttsSpeed)
                                                    }
                                                }
                                            },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = if (ttsState.isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier
                                                .size(38.dp)
                                                .testTag("tts_player_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (ttsState.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (ttsState.isSpeaking) "Durdur" else "Oynat",
                                                tint = if (ttsState.isSpeaking) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        AssistChip(
                                            onClick = {
                                                ttsLanguage = if (ttsLanguage == "tr-TR") "en-US" else "tr-TR"
                                                if (ttsState.isSpeaking) {
                                                    val fullText = buildString {
                                                        if (title.isNotBlank()) append(title.trim()).append(". ")
                                                        if (contentValue.text.isNotBlank()) append(contentValue.text.trim())
                                                    }
                                                    viewModel.ttsHelper.speak(fullText, ttsLanguage, ttsSpeed)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = if (ttsLanguage == "tr-TR") "Türkçe" else "English",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                            },
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }

                                    // Speed Selector (0.75x, 1.0x, 1.25x, 1.5x)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                                            val isSelected = ttsSpeed == spd
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    ttsSpeed = spd
                                                    viewModel.ttsHelper.setSpeed(spd)
                                                    if (ttsState.isSpeaking) {
                                                        val fullText = buildString {
                                                            if (title.isNotBlank()) append(title.trim()).append(". ")
                                                            if (contentValue.text.isNotBlank()) append(contentValue.text.trim())
                                                        }
                                                        viewModel.ttsHelper.speak(fullText, ttsLanguage, spd)
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = "${spd}x",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Title Input
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Not Başlığı", style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // VOICE DICTATION CARD (Positioned near the middle/upper area of the screen)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("voice_dictation_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (voiceState.isListening) {
                                Text(
                                    text = "Konuşun, dinleniyor...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Soundwave graphic using our premium WaveformIndicator
                                WaveformIndicator(
                                    isListening = true,
                                    rmsDb = voiceState.rmsDb,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                if (voiceState.partialText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${voiceState.partialText}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "Eller Serbest Yazdır",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                WaveformIndicator(
                                    isListening = false,
                                    rmsDb = 0f,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            // Display Speech Error if any
                            voiceState.error?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Target selection chips & speech command help button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = !dictationTargetTitle,
                                        onClick = { dictationTargetTitle = false },
                                        label = { Text("📝 Not İçeriği", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier.height(30.dp)
                                    )
                                    FilterChip(
                                        selected = dictationTargetTitle,
                                        onClick = { dictationTargetTitle = true },
                                        label = { Text("🏷️ Not Başlığı", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier.height(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showVoiceHelpDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Sesli Komut İpuçları",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (showVoiceHelpDialog) {
                                AlertDialog(
                                    onDismissRequest = { showVoiceHelpDialog = false },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sesli Dikte Komutları")
                                        }
                                    },
                                    text = {
                                        Column(
                                            modifier = Modifier.verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Android SpeechRecognizer ile konuşurken aşağıdaki sözcükleri söyleyebilirsiniz:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("• \"Nokta\" ➔ .", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text("• \"Virgül\" ➔ ,", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text("• \"Yeni satır\" / \"Alt satır\" ➔ Yeni Paragraf", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text("• \"Soru işareti\" ➔ ?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text("• \"Ünlem\" ➔ !", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text("• \"İki nokta\" ➔ :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("• \"Notu kaydet\" ➔ Notu otomatik kaydeder", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    Text("• \"Temizle\" ➔ Metni temizler", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = { showVoiceHelpDialog = false }) {
                                            Text("Anladım")
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Language Switcher
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = "Dil",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeLangCode == "tr-TR") "Türkçe" else "English",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.clickable {
                                            activeLangCode = if (activeLangCode == "tr-TR") "en-US" else "tr-TR"
                                        }
                                    )
                                }

                                // Dynamic Record Button with ripple circle
                                val scale by animateFloatAsState(if (voiceState.isListening) 1.2f else 1.0f, label = "buttonScale")
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (voiceState.isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                        )
                                        .clickable {
                                            if (!hasRecordPermission) {
                                                requestRecordPermission()
                                            } else {
                                                if (voiceState.isListening) {
                                                    viewModel.parser.stopListening()
                                                } else {
                                                    viewModel.parser.startListening(activeLangCode, continuous = true)
                                                }
                                            }
                                        }
                                        .testTag("dictate_button")
                                ) {
                                    Icon(
                                        imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (voiceState.isListening) "Kaydı Durdur" else "Sesle Yazdır",
                                        tint = if (voiceState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Text Instructions helper
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Konuştuğunuz kelimeler imlecin olduğu yere otomatik eklenir.",
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Rich Text Markdown Toolbar & Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Bold
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleMarkdownWrap(contentValue, "**") },
                                modifier = Modifier.size(34.dp).testTag("md_bold_btn")
                            ) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Kalın (**)", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            // Italic
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleMarkdownWrap(contentValue, "*") },
                                modifier = Modifier.size(34.dp).testTag("md_italic_btn")
                            ) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Eğik (*)", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            // Header 1
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleLinePrefix(contentValue, "# ") },
                                modifier = Modifier.size(34.dp).testTag("md_h1_btn")
                            ) {
                                Text("#1", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            // Header 2
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleLinePrefix(contentValue, "## ") },
                                modifier = Modifier.size(34.dp).testTag("md_h2_btn")
                            ) {
                                Text("#2", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            // Bullet List
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleLinePrefix(contentValue, "- ") },
                                modifier = Modifier.size(34.dp).testTag("md_bullet_btn")
                            ) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = "Madde İşareti (-)", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            // Strikethrough
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleMarkdownWrap(contentValue, "~~") },
                                modifier = Modifier.size(34.dp).testTag("md_strike_btn")
                            ) {
                                Icon(Icons.Default.FormatStrikethrough, contentDescription = "Üstü Çizili (~~)", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            // Inline Code
                            IconButton(
                                onClick = { contentValue = RichTextHelper.toggleMarkdownWrap(contentValue, "`") },
                                modifier = Modifier.size(34.dp).testTag("md_code_btn")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "Kod Block (`)", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Markdown Live Preview Toggle Button
                        FilterChip(
                            selected = isMarkdownPreviewMode,
                            onClick = { isMarkdownPreviewMode = !isMarkdownPreviewMode },
                            label = {
                                Text(
                                    text = if (isMarkdownPreviewMode) "Düzenle" else "Önizle",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isMarkdownPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("toggle_markdown_preview_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isMarkdownPreviewMode) {
                        // Rendered Markdown Live Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("markdown_live_preview")
                        ) {
                            if (contentValue.text.isBlank()) {
                                Text(
                                    text = "Önizlenecek Markdown içeriği bulunmuyor...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            } else {
                                Text(
                                    text = RichTextHelper.parseHtmlToAnnotatedString(
                                        text = contentValue.text,
                                        primaryColor = MaterialTheme.colorScheme.primary,
                                        header2Color = MaterialTheme.colorScheme.onSurface,
                                        header3Color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                        codeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Content Input
                        TextField(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            placeholder = { Text("Notunuzu yazın veya Markdown biçimlendirme araçlarını kullanın...", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 180.dp)
                                .testTag("note_content_input")
                        )
                    }
                }


            }
        }

        // AI Assistant Bottom Sheet
        if (showAiBottomSheet) {
            AiAssistantBottomSheet(
                currentContent = contentValue.text,
                viewModel = viewModel,
                onAppendResult = { newText ->
                    val current = contentValue.text
                    val combined = if (current.isBlank()) newText else "$current\n\n$newText"
                    contentValue = TextFieldValue(combined, androidx.compose.ui.text.TextRange(combined.length))
                },
                onReplaceResult = { newText ->
                    contentValue = TextFieldValue(newText, androidx.compose.ui.text.TextRange(newText.length))
                },
                onEventSelected = { event ->
                    // Set reminder from detected event
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val dateStr = if (event.time != null) "${event.date} ${event.time}" else "${event.date} 09:00"
                        val date = sdf.parse(dateStr)
                        reminderTimeState = date?.time
                        Toast.makeText(context, "Hatırlatıcı ayarlandı: ${event.title}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onDismiss = { 
                    viewModel.clearDetectedEvents()
                    showAiBottomSheet = false 
                }
            )
        }

        // Drawing & Sketching Canvas Dialog
        if (showDrawingCanvasDialog) {
            DrawingCanvasDialog(
                initialImagePath = drawingPathState,
                onDismiss = { showDrawingCanvasDialog = false },
                onSave = { savedFilePath ->
                    drawingPathState = savedFilePath
                    showDrawingCanvasDialog = false
                    Toast.makeText(context, "Çizim nota eklendi!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Folder Selection Dialog
        if (showFolderSelectionDialog) {
            val existingFolders by viewModel.folders.collectAsStateWithLifecycle()
            var newFolderName by remember { mutableStateOf("") }
            var isCreatingNewFolder by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showFolderSelectionDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Klasör Seç")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bu notu hangi klasöre eklemek istersiniz?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Genel (No Folder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    folderState = null
                                    showFolderSelectionDialog = false
                                }
                                .background(if (folderState == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Genel (Klasörsüz)", fontWeight = if (folderState == null) FontWeight.Bold else FontWeight.Normal)
                        }

                        for (f in existingFolders) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        folderState = f
                                        showFolderSelectionDialog = false
                                    }
                                    .background(if (folderState == f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(f, fontWeight = if (folderState == f) FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        if (!isCreatingNewFolder) {
                            OutlinedButton(
                                onClick = { isCreatingNewFolder = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni Klasör Oluştur")
                            }
                        } else {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                placeholder = { Text("Yeni klasör adı...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isCreatingNewFolder = false; newFolderName = "" }) {
                                    Text("İptal")
                                }
                                Button(
                                    onClick = {
                                        if (newFolderName.isNotBlank()) {
                                            folderState = newFolderName.trim()
                                            showFolderSelectionDialog = false
                                        }
                                    }
                                ) {
                                    Text("Seç & Kaydet")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFolderSelectionDialog = false }) {
                        Text("Kapat")
                    }
                }
            )
        }

        // PIN Setup Dialog when locking note for the first time
        if (showPinDialogForLock) {
            PinLockDialog(
                mode = PinDialogMode.SET_NEW_PIN,
                title = "Not Güvenliği İçin PIN Belirleyin",
                onSuccess = {
                    isLockedState = true
                    showPinDialogForLock = false
                    Toast.makeText(context, "PIN oluşturuldu ve not kilitlendi!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showPinDialogForLock = false }
            )
        }
    }
}

@Composable
fun QuickVoiceNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tagIds: List<Int>) -> Unit,
    viewModel: NoteViewModel,
    hasRecordPermission: Boolean,
    requestRecordPermission: () -> Unit,
    availableTags: List<Tag> = emptyList()
) {
    val voiceState by viewModel.parser.state.collectAsStateWithLifecycle()
    var activeLangCode by remember { mutableStateOf("tr-TR") }
    val defaultTitle = remember {
        "Sesli Not • " + SimpleDateFormat("d MMMM HH:mm", Locale("tr")).format(Date())
    }
    var noteTitle by remember { mutableStateOf(defaultTitle) }
    var capturedText by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf(availableTags.firstOrNull()?.id) }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                capturedText = viewModel.parser.appendTranscription(capturedText, text, activeLangCode)
            }
        }
    }

    // Start listening on launch if permission granted
    LaunchedEffect(hasRecordPermission) {
        if (hasRecordPermission) {
            viewModel.parser.startListening(activeLangCode, continuous = true)
        } else {
            requestRecordPermission()
        }
    }

    // Append speech recognition text seamlessly when results are received
    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotBlank()) {
            capturedText = viewModel.parser.appendTranscription(capturedText, voiceState.spokenText, activeLangCode)
            viewModel.parser.clearSpokenText()
        }
    }

    // Handle hands-free voice command actions (e.g. saying "Kaydet" / "Notu kaydet" or "Metni temizle")
    LaunchedEffect(voiceState.triggeredAction) {
        when (voiceState.triggeredAction) {
            VoiceCommandAction.SAVE -> {
                viewModel.parser.clearAction()
                viewModel.parser.stopListening()
                val finalTitle = noteTitle.trim().ifBlank { defaultTitle }
                val finalContent = capturedText.trim()
                if (finalContent.isNotBlank() || finalTitle.isNotBlank()) {
                    onSave(finalTitle, finalContent, selectedTagId?.let { listOf(it) } ?: emptyList())
                }
            }
            VoiceCommandAction.CLEAR -> {
                viewModel.parser.clearAction()
                capturedText = ""
            }
            VoiceCommandAction.TITLE -> {
                val newTitle = voiceState.commandPayload ?: ""
                viewModel.parser.clearAction()
                if (newTitle.isNotBlank()) {
                    noteTitle = newTitle
                }
            }
            VoiceCommandAction.TAG -> {
                val newTag = voiceState.commandPayload ?: ""
                viewModel.parser.clearAction()
                if (newTag.isNotBlank()) {
                    val foundTag = availableTags.find { it.name.equals(newTag, ignoreCase = true) }
                    if (foundTag != null) {
                        selectedTagId = foundTag.id
                    }
                }
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.parser.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("quick_voice_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon, title and language toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Sesli Not Oluştur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Language toggle pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                activeLangCode = if (activeLangCode == "tr-TR") "en-US" else "tr-TR"
                                if (voiceState.isListening) {
                                    viewModel.parser.startListening(activeLangCode)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Dil Değiştir",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeLangCode == "tr-TR") "Türkçe" else "English",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title input field
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Not Başlığı") },
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (noteTitle.isNotEmpty()) {
                            IconButton(onClick = { noteTitle = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Transcribed content field (editable & real-time)
                OutlinedTextField(
                    value = capturedText,
                    onValueChange = { capturedText = it },
                    label = { Text("Not İçeriği (Sesle Yazdırılıyor)") },
                    placeholder = {
                        Text(
                            if (voiceState.isListening) "Konuşmaya başlayın, dinleniyor..." else "Konuşmak için aşağıdaki mikrofona dokunun...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (capturedText.isNotEmpty()) {
                            IconButton(onClick = {
                                capturedText = ""
                                viewModel.parser.clearSpokenText()
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Metni Temizle", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp)
                )

                // Live partial transcript subtitle
                if (voiceState.isListening && voiceState.partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Konuşuluyor: \"${voiceState.partialText}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Error Message if speech recognizer encountered an issue
                voiceState.error?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Row {
                            TextButton(
                                onClick = {
                                    systemSpeechLauncher.launch(viewModel.parser.createSpeechIntent(activeLangCode))
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Sistem Dikte", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = {
                                    viewModel.parser.clearError()
                                    viewModel.parser.startListening(activeLangCode, continuous = true)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Tekrar Dene", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Voice command helpful hint pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "💡 'Nokta', 'yeni satır', 'Başlık Toplantı', 'Etiket İş' diyebilir; 'Notu kaydet' veya 'Temizle' komutlarını verebilirsiniz.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Voice Dictation Test / Demo Chips (Useful for emulator without mic input)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚡ Önizleme / Test Ses Örnekleri:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.parser.simulateSpokenText("Yarın saat onda proje değerlendirme toplantısı yapılacak nokta Yeni satır Katılımcılar sunumlarını hazırlasın nokta")
                                },
                                label = { Text("🎙️ Toplantı", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.parser.simulateSpokenText("Market alışverişi virgül süt virgül yumurta ve taze ekmek alınacak nokta")
                                },
                                label = { Text("🛒 Alışveriş", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.parser.simulateSpokenText("Bugün tamamlanacak önemli görevler virgül rapor gönderimi yapıldı nokta Notu kaydet")
                                },
                                label = { Text("💾 Kaydet Komutu", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound wave visualizer responding to voice volume (rmsDb)
                WaveformIndicator(
                    isListening = voiceState.isListening,
                    rmsDb = voiceState.rmsDb,
                    color = if (voiceState.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Microphone control button
                val buttonScale by animateFloatAsState(
                    targetValue = if (voiceState.isListening) 1.12f else 1f,
                    label = "micScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState.isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                            )
                            .clickable {
                                if (!hasRecordPermission) {
                                    requestRecordPermission()
                                } else {
                                    if (voiceState.isListening) {
                                        viewModel.parser.stopListening()
                                    } else {
                                        viewModel.parser.startListening(activeLangCode, continuous = true)
                                    }
                                }
                            }
                            .testTag("quick_voice_record_toggle")
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (voiceState.isListening) "Kaydı Durdur" else "Konuşmaya Başla",
                            tint = if (voiceState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (voiceState.isListening) "Dinleniyor... (Durdurmak için dokunun)" else "Konuşmak için mikrofona dokunun",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (voiceState.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (voiceState.isListening) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Tag Selection Chips
                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Etiket Seçin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableTags) { tag ->
                            val isSelected = selectedTagId == tag.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTagId = if (isSelected) null else tag.id },
                                label = { Text(tag.name, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.parser.stopListening()
                            onDismiss()
                        }
                    ) {
                        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.parser.stopListening()
                            val finalTitle = noteTitle.trim().ifBlank { defaultTitle }
                            val finalContent = capturedText.trim()
                            onSave(finalTitle, finalContent, selectedTagId?.let { listOf(it) } ?: emptyList())
                        },
                        enabled = capturedText.isNotBlank() || noteTitle.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_quick_voice_note_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Kaydet", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Notu Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentNoteCard(
    item: NoteWithTags,
    hasWallpaper: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note = item.note
    val cardBg = if (hasWallpaper) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val titleColor = ColorUtils.getPrimaryTitleColor(cardBg)
    val bodyColor = ColorUtils.getBodyContentColor(cardBg)
    val subtleColor = ColorUtils.getSubtleMetadataColor(cardBg)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .width(200.dp)
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("recent_note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title.ifBlank { "Başlıksız Not" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Sabitlendi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val cleanText = RichTextHelper.parseHtmlToAnnotatedString(note.content).text
                if (cleanText.isNotBlank()) {
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.bodySmall.copy(color = bodyColor),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.tags.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = item.tags.first().name + if (item.tags.size > 1) " +${item.tags.size - 1}" else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val diff = System.currentTimeMillis() - note.createdAt
                val minutes = diff / (1000 * 60)
                val hours = diff / (1000 * 60 * 60)
                val timeStr = when {
                    minutes < 1 -> "Az önce"
                    minutes < 60 -> "$minutes dk"
                    else -> "$hours sa"
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(color = subtleColor)
                )
            }
        }
    }
}
