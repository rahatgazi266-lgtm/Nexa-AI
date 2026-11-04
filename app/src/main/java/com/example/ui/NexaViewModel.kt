package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.model.AppUpdateInfo
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.MemoryItem
import com.example.model.UpdateState
import com.example.service.ChatSessionService
import com.example.service.MemoryVaultService
import com.example.service.NexaBrainService
import com.example.service.SecurityScannerService
import com.example.service.UpdateManager
import com.example.service.UserDataBackupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class DialogType {
    SETTINGS, SECURITY, MEMORY_VAULT, RECENT_CHATS, ABOUT, UPDATE, BACKUP_RESTORE
}

class NexaViewModel(application: Application) : AndroidViewModel(application) {
    private val brainService = NexaBrainService(application)
    private val memoryService = MemoryVaultService(application)
    private val sessionService = ChatSessionService(application)
    private val updateManager = UpdateManager(application)
    private val backupService = UserDataBackupService(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName: StateFlow<String?> = _attachedFileName.asStateFlow()

    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification: StateFlow<String?> = _statusNotification.asStateFlow()

    val updateState: StateFlow<UpdateState> = updateManager.updateState

    private var currentSessionId: String = System.currentTimeMillis().toString()

    init {
        loadData()
        // Listen to update state to trigger update dialog when available
        viewModelScope.launch {
            updateManager.updateState.collect { state ->
                when (state) {
                    is UpdateState.Available -> {
                        openDialog(DialogType.UPDATE)
                    }
                    is UpdateState.UpToDate -> {
                        _statusNotification.value = "Nexa AI is up to date (v${state.currentVersion})"
                    }
                    is UpdateState.Error -> {
                        _statusNotification.value = state.message
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadData() {
        _sessions.value = sessionService.getAllSessions()
        _memories.value = memoryService.getAllMemories()
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun openDialog(dialog: DialogType) {
        loadData()
        _activeDialog.value = dialog
    }

    fun closeDialog() {
        if (_activeDialog.value == DialogType.UPDATE) {
            updateManager.resetState()
        }
        _activeDialog.value = null
    }

    fun clearNotification() {
        _statusNotification.value = null
    }

    fun attachFile(fileName: String, sizeBytes: Long) {
        val scan = SecurityScannerService.validateAttachment(fileName, sizeBytes)
        if (!scan.safe) {
            _statusNotification.value = "Security Alert: ${scan.threats.firstOrNull()?.message ?: "Blocked"}"
            return
        }
        _attachedFileName.value = fileName
        _statusNotification.value = "Attached: $fileName"
    }

    fun removeAttachment() {
        _attachedFileName.value = null
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val attachment = _attachedFileName.value
        if (text.isEmpty() && attachment == null) return
        if (_isGenerating.value) return

        val messageText = text.ifEmpty { "Analyzing attached file: $attachment" }

        // Security scan before processing
        val scanResult = SecurityScannerService.scanText(messageText)
        if (!scanResult.safe) {
            _statusNotification.value = "Security Guard: ${scanResult.threats.firstOrNull()?.message ?: "Suspicious content"}"
        }

        val userMessage = ChatMessage(
            text = messageText,
            isUser = true,
            attachmentName = attachment
        )

        val updated = _messages.value.toMutableList()
        updated.add(userMessage)
        _messages.value = updated

        _inputText.value = ""
        _attachedFileName.value = null
        _isGenerating.value = true

        // Auto learn personal memory facts
        memoryService.tryAutoCapture(messageText)
        _memories.value = memoryService.getAllMemories()

        viewModelScope.launch {
            try {
                val memoryTexts = _memories.value.map { it.content }
                val reply = brainService.generateResponse(messageText, updated, memoryTexts)

                val assistantMessage = ChatMessage(
                    text = reply,
                    isUser = false
                )
                val finalMessages = _messages.value.toMutableList()
                finalMessages.add(assistantMessage)
                _messages.value = finalMessages

                // Auto save session
                val sessionTitle = finalMessages.firstOrNull { it.isUser }?.text?.take(28) ?: "Conversation"
                sessionService.saveSession(
                    ChatSession(
                        id = currentSessionId,
                        title = sessionTitle,
                        messages = finalMessages
                    )
                )
                _sessions.value = sessionService.getAllSessions()
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "⚠️ **Error:** ${e.localizedMessage ?: e.message ?: "An unexpected issue occurred."}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun selectSuggestion(prompt: String) {
        _inputText.value = prompt
        sendMessage()
    }

    fun startNewChat() {
        _messages.value = emptyList()
        _inputText.value = ""
        _attachedFileName.value = null
        currentSessionId = System.currentTimeMillis().toString()
        _statusNotification.value = "Started new chat"
    }

    fun loadSession(session: ChatSession) {
        _messages.value = session.messages
        currentSessionId = session.id
        closeDialog()
        _statusNotification.value = "Loaded conversation: ${session.title}"
    }

    fun deleteSession(sessionId: String) {
        sessionService.deleteSession(sessionId)
        _sessions.value = sessionService.getAllSessions()
    }

    fun getApiKey(): String = brainService.getSavedApiKey()

    fun saveApiKey(key: String) {
        brainService.saveApiKey(key)
        _statusNotification.value = "Saved Google Gemini API key"
    }

    fun removeApiKey() {
        brainService.removeApiKey()
        _statusNotification.value = "Removed API key."
    }

    fun clearChatHistory() {
        sessionService.clearAll()
        _sessions.value = emptyList()
        _messages.value = emptyList()
        _statusNotification.value = "Cleared chat history"
    }

    fun clearMemoryVault() {
        memoryService.clearAllMemories()
        _memories.value = emptyList()
        _statusNotification.value = "Cleared Memory Vault"
    }

    fun deleteMemory(id: String) {
        memoryService.deleteMemory(id)
        _memories.value = memoryService.getAllMemories()
    }

    fun clearTemporaryMemories() {
        memoryService.clearTemporaryMemories()
        _memories.value = memoryService.getAllMemories()
        _statusNotification.value = "Cleared temporary memories"
    }

    fun setReaction(messageId: String, isLiked: Boolean) {
        val updated = _messages.value.map {
            if (it.id == messageId) {
                it.copy(liked = if (it.liked == isLiked) null else isLiked)
            } else it
        }
        _messages.value = updated
        _statusNotification.value = if (isLiked) "Feedback: Liked!" else "Feedback: Noted!"
    }

    // --- In-App Update Actions ---
    fun checkForUpdates(manual: Boolean = true) {
        if (manual) {
            openDialog(DialogType.UPDATE)
        }
        viewModelScope.launch {
            updateManager.checkForUpdates(manual = manual)
        }
    }

    fun downloadUpdate(updateInfo: AppUpdateInfo) {
        viewModelScope.launch {
            updateManager.downloadAndPrepareInstall(updateInfo)
        }
    }

    fun installUpdate(apkFile: File) {
        updateManager.launchPackageInstaller(apkFile)
    }

    // --- User Data Backup & Restore ---
    fun exportBackupJson(): String {
        return backupService.createBackupJson()
    }

    fun restoreBackupJson(jsonString: String): Boolean {
        val success = backupService.restoreBackupJson(jsonString)
        if (success) {
            loadData()
            _statusNotification.value = "Data successfully restored!"
        } else {
            _statusNotification.value = "Failed to restore: Invalid backup format."
        }
        return success
    }
}
