package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.ui.components.EmojiKeyboardPanel
import com.example.ui.components.MessageBubble
import com.example.ui.components.NexaSaturnLogo
import com.example.ui.components.SuggestionsGrid
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.dialogs.AboutDialog
import com.example.ui.dialogs.BackupRestoreDialog
import com.example.ui.dialogs.MemoryVaultDialog
import com.example.ui.dialogs.RecentChatsDialog
import com.example.ui.dialogs.SecurityDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.UpdateDialog
import com.example.ui.theme.NexaBlack
import com.example.ui.theme.NexaDarkGreen
import com.example.ui.theme.NexaGreenBorder
import com.example.ui.theme.NexaGreenBorderBright
import com.example.ui.theme.NexaGreenDark
import com.example.ui.theme.NexaNeonGreen
import com.example.ui.theme.NexaSurface
import com.example.ui.theme.NexaSurfaceVariant
import com.example.ui.theme.NexaTextMuted
import com.example.ui.theme.NexaTextSubtle
import com.example.ui.theme.NexaTextWhite
import kotlinx.coroutines.launch

@Composable
fun NexaHomeScreen(
    viewModel: NexaViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val attachedFile by viewModel.attachedFileName.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val notification by viewModel.statusNotification.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var showEmojiPanel by remember { mutableStateOf(false) }

    // Close drawer on Android back press if open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    // Show status toasts/snackbars
    LaunchedEffect(notification) {
        notification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // Auto scroll on new messages
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // File picker contract for attachment button
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "attachment"
            var fileSize = 0L
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            viewModel.attachFile(fileName, fileSize)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NexaDarkGreen,
                drawerContentColor = NexaTextWhite,
                modifier = Modifier
                    .width(320.dp)
                    .border(
                        width = 1.dp,
                        color = NexaGreenBorder,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(18.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        NexaSaturnLogo(size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NEXA AI",
                                color = NexaTextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Developer: MD.Rahat Gazi",
                                color = NexaNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // + New Chat button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexaGreenDark)
                            .border(1.dp, NexaGreenBorderBright, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("drawer_new_chat_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NexaNeonGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Chat",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "WORKSPACE",
                        color = NexaTextSubtle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        label = "Recent Chats",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.RECENT_CHATS)
                        }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Psychology,
                        label = "Memory Vault (${memories.size})",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.MEMORY_VAULT)
                        }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Backup,
                        label = "Backup & Restore",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.BACKUP_RESTORE)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NexaGreenBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SECURITY",
                        color = NexaTextSubtle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Security,
                        label = "Security Shield (100%)",
                        iconTint = NexaNeonGreen,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.SECURITY)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NexaGreenBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "PREFERENCES & UPDATES",
                        color = NexaTextSubtle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Outlined.SystemUpdate,
                        label = "Check for Updates",
                        iconTint = NexaNeonGreen,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.checkForUpdates(manual = true)
                        }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Settings,
                        label = "Settings & API Key",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.SETTINGS)
                        }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Info,
                        label = "About Nexa AI",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openDialog(DialogType.ABOUT)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Drawer footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NEXA AI • v${BuildConfig.VERSION_NAME}",
                            color = NexaTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Developer: MD.Rahat Gazi",
                            color = NexaNeonGreen,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = NexaBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xF0030805))
                        .border(
                            width = 1.dp,
                            color = Color(0x1AFFFFFF),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { coroutineScope.launch { drawerState.open() } },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = Color(0xFFD5E5DB)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        NexaSaturnLogo(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NEXA AI",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NexaNeonGreen, CircleShape)
                                        .shadow(4.dp, CircleShape, spotColor = NexaNeonGreen)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Developer: MD.Rahat Gazi",
                                    color = NexaTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Top Action: New Chat
                    IconButton(
                        onClick = { viewModel.startNewChat() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_new_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = NexaNeonGreen
                        )
                    }
                }

                // Chat Messages or Welcome Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        // Welcome Hero View - Clean without technical/model clutter
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            NexaSaturnLogo(size = 80.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "How can I help you?",
                                color = NexaTextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "I am Nexa AI — your personal AI assistant.\nDeveloper: MD.Rahat Gazi",
                                color = NexaTextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            SuggestionsGrid(
                                onSelect = { viewModel.selectSuggestion(it) }
                            )
                        }
                    } else {
                        // Message list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                MessageBubble(
                                    message = msg,
                                    onReaction = { isLiked ->
                                        viewModel.setReaction(msg.id, isLiked)
                                    }
                                )
                            }

                            if (isGenerating) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NexaSaturnLogo(size = 30.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TypingDotsIndicator()
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // Emoji Panel dropdown if open
                AnimatedVisibility(
                    visible = showEmojiPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    EmojiKeyboardPanel(
                        onEmojiSelected = { emoji ->
                            viewModel.onInputChanged(inputText + emoji)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Attachment badge if attached
                if (attachedFile != null) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(NexaSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, NexaGreenBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = NexaNeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = attachedFile!!,
                            color = NexaTextWhite,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = NexaTextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.removeAttachment() }
                        )
                    }
                }

                // Composer Bar
                Surface(
                    color = NexaSurfaceVariant,
                    shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NexaGreenBorderBright),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // Emoji Button
                        IconButton(
                            onClick = { showEmojiPanel = !showEmojiPanel },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Text("😀", fontSize = 18.sp)
                        }

                        // Attach Button
                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = NexaTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text input field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            if (inputText.isEmpty() && attachedFile == null) {
                                Text(
                                    text = "Ask Nexa something...",
                                    color = NexaTextSubtle,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { viewModel.onInputChanged(it) },
                                textStyle = TextStyle(
                                    color = NexaTextWhite,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(NexaNeonGreen),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("prompt_input")
                            )
                        }

                        // Radiant Glowing Circular Send Button
                        val canSend = (inputText.isNotBlank() || attachedFile != null) && !isGenerating
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFAAFFCE),
                                                Color(0xFF16ED77),
                                                Color(0xFF00A94E),
                                                Color(0xFF006B32)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF1B2B20), Color(0xFF121F17))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (canSend) Color(0xFF9DFFC3) else NexaGreenBorder,
                                    shape = CircleShape
                                )
                                .clickable(enabled = canSend) {
                                    showEmojiPanel = false
                                    viewModel.sendMessage()
                                }
                                .testTag("send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else NexaTextSubtle,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Footer branding hint
                Text(
                    text = "Nexa AI • Developer: MD.Rahat Gazi",
                    color = NexaTextSubtle,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }
        }
    }

    // Dialogs
    when (activeDialog) {
        DialogType.SETTINGS -> {
            SettingsDialog(
                currentApiKey = viewModel.getApiKey(),
                onSaveApiKey = { viewModel.saveApiKey(it) },
                onRemoveApiKey = { viewModel.removeApiKey() },
                onClearChatHistory = { viewModel.clearChatHistory() },
                onClearMemoryVault = { viewModel.clearMemoryVault() },
                onCheckForUpdates = {
                    viewModel.closeDialog()
                    viewModel.checkForUpdates(manual = true)
                },
                onOpenBackupRestore = {
                    viewModel.closeDialog()
                    viewModel.openDialog(DialogType.BACKUP_RESTORE)
                },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.UPDATE -> {
            UpdateDialog(
                updateState = updateState,
                onUpdateNow = { viewModel.downloadUpdate(it) },
                onInstallNow = { viewModel.installUpdate(it) },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.BACKUP_RESTORE -> {
            BackupRestoreDialog(
                onExportBackup = { viewModel.exportBackupJson() },
                onRestoreBackup = { viewModel.restoreBackupJson(it) },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.SECURITY -> {
            SecurityDialog(
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.MEMORY_VAULT -> {
            MemoryVaultDialog(
                memories = memories,
                onDeleteMemory = { viewModel.deleteMemory(it) },
                onClearTemporary = { viewModel.clearTemporaryMemories() },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.RECENT_CHATS -> {
            RecentChatsDialog(
                sessions = sessions,
                onSelectSession = { viewModel.loadSession(it) },
                onDeleteSession = { viewModel.deleteSession(it) },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.ABOUT -> {
            AboutDialog(
                onDismiss = { viewModel.closeDialog() }
            )
        }
        null -> {}
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = NexaTextMuted,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = NexaTextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
