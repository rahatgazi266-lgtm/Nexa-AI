package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.model.AppUpdateInfo
import com.example.model.ChatSession
import com.example.model.MemoryItem
import com.example.model.UpdateState
import com.example.service.SecurityScannerService
import com.example.ui.components.NexaSaturnLogo
import com.example.ui.theme.NexaAmber
import com.example.ui.theme.NexaCardBg
import com.example.ui.theme.NexaGreenBorder
import com.example.ui.theme.NexaGreenBorderBright
import com.example.ui.theme.NexaGreenDark
import com.example.ui.theme.NexaGreenMid
import com.example.ui.theme.NexaNeonGreen
import com.example.ui.theme.NexaRed
import com.example.ui.theme.NexaRedBg
import com.example.ui.theme.NexaRedBorder
import com.example.ui.theme.NexaSurface
import com.example.ui.theme.NexaSurfaceVariant
import com.example.ui.theme.NexaTextMuted
import com.example.ui.theme.NexaTextSubtle
import com.example.ui.theme.NexaTextWhite
import java.io.File

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onRemoveApiKey: () -> Unit,
    onClearChatHistory: () -> Unit,
    onClearMemoryVault: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Settings & API Key",
                        color = NexaTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Developer Attribution Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "DEVELOPER ATTRIBUTION",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Developer: MD.Rahat Gazi",
                        color = NexaTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API Key Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "GOOGLE GEMINI API KEY",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI Brain: Google Gemini 3.5 Flash-Lite (Free Tier: 15 RPM / 500 RPD).",
                        color = NexaTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("AIzaSy...", color = NexaTextSubtle, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexaNeonGreen,
                            unfocusedBorderColor = NexaGreenBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveApiKey(keyInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_key_btn")
                        ) {
                            Text("Save Key", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                keyInput = ""
                                onRemoveApiKey()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexaRedBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NexaRedBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Remove", color = NexaRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (keyInput.isNotBlank()) "🟢 Google Gemini AI Brain Active" else "⚠️ API Key Not Configured (Setup in Settings)",
                        color = if (keyInput.isNotBlank()) NexaNeonGreen else NexaAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Updates & Backup row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCheckForUpdates,
                        colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🚀 Check Updates", color = NexaTextWhite, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onOpenBackupRestore,
                        colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💾 Backup & Restore", color = NexaTextWhite, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Storage Maintenance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "MAINTENANCE & RESET",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onClearChatHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Chat History", color = NexaTextWhite, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onClearMemoryVault,
                        colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Memory Vault", color = NexaTextWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onUpdateNow: (AppUpdateInfo) -> Unit,
    onInstallNow: (File) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚀", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nexa AI Update",
                            color = NexaTextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (updateState) {
                    is UpdateState.Checking -> {
                        CircularProgressIndicator(color = NexaNeonGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Checking for updates...", color = NexaTextWhite, fontSize = 13.sp)
                    }

                    is UpdateState.Available -> {
                        val info = updateState.updateInfo
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "A newer version is available!",
                                color = NexaNeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "New Version: ${info.versionName}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Current Version: v${BuildConfig.VERSION_NAME}",
                                color = NexaTextMuted,
                                fontSize = 11.sp
                            )
                            if (info.apkSize > 0) {
                                Text(
                                    text = "Size: ${info.apkSize / (1024 * 1024)} MB",
                                    color = NexaTextSubtle,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Changelog:",
                                color = NexaTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(NexaSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn {
                                    item {
                                        Text(
                                            text = info.changelog,
                                            color = NexaTextWhite,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Later", color = NexaTextWhite, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { onUpdateNow(info) },
                                colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Update Now", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Downloading Update...",
                                color = NexaNeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NexaNeonGreen,
                                trackColor = NexaSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            val dlMb = updateState.downloadedBytes / (1024f * 1024f)
                            val totalMb = updateState.totalBytes / (1024f * 1024f)
                            Text(
                                text = "${String.format("%.1f", dlMb)} MB / ${String.format("%.1f", totalMb)} MB (${updateState.progressPercent}%)",
                                color = NexaTextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        val verif = updateState.verificationResult
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛡️ Security Verification Passed", color = NexaNeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NexaSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("✓ Package ID: ${verif.packageName}", color = Color.White, fontSize = 11.sp)
                                Text("✓ Signing Certificate: Verified Match", color = NexaNeonGreen, fontSize = 11.sp)
                                Text("✓ APK SHA-256: ${verif.fileSha256.take(24)}...", color = NexaTextMuted, fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Package is verified authentic and safe. Tap below to launch the official Android installer to complete the update.",
                                color = NexaTextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onInstallNow(updateState.apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Approve & Install Update", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateState.UpToDate -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨ Up to Date", color = NexaNeonGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You are already using the latest version of Nexa AI (v${updateState.currentVersion}).",
                                color = NexaTextWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateState.NoReleaseFound -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📦 No Release Found (HTTP 404)", color = NexaNeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No GitHub Release found for repository '${updateState.repository}' yet.",
                                color = NexaTextWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "When a release containing 'Nexa-AI.apk' is published to GitHub, you will be able to update directly from here.",
                                color = NexaTextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss", color = NexaTextWhite)
                            }
                        }
                    }

                    is UpdateState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexaCardBg, RoundedCornerShape(12.dp))
                                .border(1.dp, NexaRedBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚠️ Update Check Note", color = NexaRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateState.message,
                                color = NexaTextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss", color = NexaTextWhite)
                            }
                        }
                    }

                    UpdateState.Idle -> {}
                }
            }
        }
    }
}

@Composable
fun BackupRestoreDialog(
    onExportBackup: () -> String,
    onRestoreBackup: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var backupJson by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💾 Backup & Restore Data",
                        color = NexaTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Backup section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "EXPORT BACKUP",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate a snapshot of all your conversations and memories.",
                        color = NexaTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val json = onExportBackup()
                            backupJson = json
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NexaBackup", json))
                            statusMsg = "Backup copied to clipboard! (${json.length} chars)"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create & Copy Backup", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Restore section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "RESTORE FROM BACKUP",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        placeholder = { Text("Paste backup JSON here...", color = NexaTextSubtle, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexaNeonGreen,
                            unfocusedBorderColor = NexaGreenBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (restoreInput.isNotBlank()) {
                                val ok = onRestoreBackup(restoreInput)
                                statusMsg = if (ok) "Data successfully restored!" else "Invalid backup JSON"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexaSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Data", color = NexaTextWhite)
                    }
                }

                if (statusMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMsg!!,
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityDialog(
    onDismiss: () -> Unit
) {
    var urlToScan by remember { mutableStateOf("") }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var isSafeUrl by remember { mutableStateOf(true) }

    val defenseScore = SecurityScannerService.getDefenseRating()
    val totalScanned = SecurityScannerService.totalScanned
    val logs = SecurityScannerService.logs

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NexaNeonGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Security Shield",
                            color = NexaTextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Score banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(14.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM DEFENSE RATING",
                            color = NexaTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$defenseScore%",
                            color = NexaNeonGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Status: Protected & Verified",
                            color = NexaTextWhite,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(NexaGreenDark, CircleShape)
                            .border(2.dp, NexaNeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick URL Scanner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "QUICK URL SAFETY SCANNER",
                        color = NexaNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = urlToScan,
                            onValueChange = { urlToScan = it },
                            placeholder = { Text("https://...", color = NexaTextSubtle, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexaNeonGreen,
                                unfocusedBorderColor = NexaGreenBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (urlToScan.isNotBlank()) {
                                    val res = SecurityScannerService.scanUrl(urlToScan)
                                    isSafeUrl = res.safe
                                    scanMessage = if (res.safe) {
                                        "Clean: No malicious pattern detected."
                                    } else {
                                        "Warning: ${res.threats.firstOrNull()?.message ?: "Suspicious URL"}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Scan", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (scanMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scanMessage!!,
                            color = if (isSafeUrl) NexaNeonGreen else NexaRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recent Security Logs
                Text(
                    text = "Recent Security Events ($totalScanned scanned)",
                    color = NexaTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF030704), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "All system activities clean. Zero threats detected.",
                                color = NexaTextSubtle,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        items(logs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("Blocked")) NexaRed else NexaNeonGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryVaultDialog(
    memories: List<MemoryItem>,
    onDeleteMemory: (String) -> Unit,
    onClearTemporary: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 Memory Vault",
                        color = NexaTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Text(
                    text = "Personal facts remembered securely on your device (${memories.size} items)",
                    color = NexaTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(NexaCardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (memories.isEmpty()) {
                        item {
                            Text(
                                text = "No memories stored yet. Mention facts in conversation (e.g. 'my name is Alex' or 'remember that...') to auto-store!",
                                color = NexaTextSubtle,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        items(memories) { mem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NexaSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = mem.content, color = NexaTextWhite, fontSize = 12.sp)
                                    Text(
                                        text = mem.type.uppercase(),
                                        color = NexaNeonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteMemory(mem.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexaRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onClearTemporary,
                    colors = ButtonDefaults.buttonColors(containerColor = NexaRedBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexaRedBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Temporary Memories", color = NexaRed, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RecentChatsDialog(
    sessions: List<ChatSession>,
    onSelectSession: (ChatSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 Recent Chats",
                        color = NexaTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sessions.isEmpty()) {
                        item {
                            Text(
                                text = "No saved conversations yet. Start chatting!",
                                color = NexaTextSubtle,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(sessions) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NexaCardBg)
                                    .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                                    .clickable { onSelectSession(s) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.title,
                                        color = NexaTextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${s.date} • ${s.messages.size} msgs",
                                        color = NexaTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSession(s.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexaRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NexaSaturnLogo(size = 54.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "NEXA AI",
                    color = NexaTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Developer: MD.Rahat Gazi",
                    color = NexaNeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexaCardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, NexaGreenBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("👤 Developer: MD.Rahat Gazi", color = NexaTextWhite, fontSize = 12.sp)
                    Text("🧠 AI Engine: Google Gemini 3.5 Flash-Lite", color = NexaTextWhite, fontSize = 12.sp)
                    Text("⚡ Core Architecture: Ocean Core v2.0", color = NexaTextWhite, fontSize = 12.sp)
                    Text("🛡️ Security: Real-Time Protection Shield", color = NexaTextWhite, fontSize = 12.sp)
                    Text("📱 Platform: Real Installable Android APK", color = NexaTextWhite, fontSize = 12.sp)
                    Text("🚀 Version: v${BuildConfig.VERSION_NAME}", color = NexaNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NexaGreenMid),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it", color = Color(0xFF001B0C), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
