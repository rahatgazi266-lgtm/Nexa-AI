package com.example.service

import android.content.Context
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.MemoryItem
import org.json.JSONArray
import org.json.JSONObject

class UserDataBackupService(private val context: Context) {
    private val sessionService = ChatSessionService(context)
    private val memoryService = MemoryVaultService(context)
    private val updateManager = UpdateManager(context)

    fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Nexa AI")
        root.put("applicationId", context.packageName)
        root.put("versionCode", BuildConfig.VERSION_CODE)
        root.put("versionName", BuildConfig.VERSION_NAME)
        root.put("timestamp", System.currentTimeMillis())
        root.put("security_notice", "API keys are protected by Android Keystore and are strictly excluded from plain text export.")

        val contentsDoc = JSONArray()
        contentsDoc.put("Chat Sessions & Full Messages History")
        contentsDoc.put("Memory Vault Facts & Categorized Memories")
        contentsDoc.put("Application Preferences & Update Repository Configuration")
        root.put("documented_data_types", contentsDoc)

        // 1. Export Sessions
        val sessions = sessionService.getAllSessions()
        val sessionsArray = JSONArray()
        for (s in sessions) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("title", s.title)
            sObj.put("date", s.date)
            val msgsArray = JSONArray()
            for (m in s.messages) {
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("text", m.text)
                mObj.put("isUser", m.isUser)
                mObj.put("timestamp", m.timestamp)
                if (m.attachmentName != null) mObj.put("attachmentName", m.attachmentName)
                if (m.liked != null) mObj.put("liked", m.liked)
                msgsArray.put(mObj)
            }
            sObj.put("messages", msgsArray)
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)

        // 2. Export Memories
        val memories = memoryService.getAllMemories()
        val memoriesArray = JSONArray()
        for (m in memories) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("content", m.content)
            mObj.put("type", m.type)
            mObj.put("timestamp", m.timestamp)
            memoriesArray.put(mObj)
        }
        root.put("memories", memoriesArray)

        // 3. Export Safe Preferences
        val prefsObj = JSONObject()
        prefsObj.put("github_repo", updateManager.getGitHubRepo())
        root.put("preferences", prefsObj)

        return root.toString(2)
    }

    fun restoreBackupJson(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)

            // Validate that this backup belongs to Nexa AI
            if (root.optString("app") != "Nexa AI") {
                return false
            }

            // Restore Sessions (Non-destructive merge)
            if (root.has("sessions")) {
                val sessionsArray = root.getJSONArray("sessions")
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    val msgsArray = sObj.optJSONArray("messages") ?: JSONArray()
                    val msgs = mutableListOf<ChatMessage>()
                    for (j in 0 until msgsArray.length()) {
                        val mObj = msgsArray.getJSONObject(j)
                        msgs.add(
                            ChatMessage(
                                id = mObj.optString("id"),
                                text = mObj.optString("text"),
                                isUser = mObj.optBoolean("isUser"),
                                timestamp = mObj.optLong("timestamp"),
                                attachmentName = if (mObj.has("attachmentName")) mObj.getString("attachmentName") else null,
                                liked = if (mObj.has("liked")) mObj.getBoolean("liked") else null
                            )
                        )
                    }
                    sessionService.saveSession(
                        ChatSession(
                            id = sObj.optString("id"),
                            title = sObj.optString("title"),
                            date = sObj.optString("date"),
                            messages = msgs
                        )
                    )
                }
            }

            // Restore Memories (Non-destructive merge)
            if (root.has("memories")) {
                val memoriesArray = root.getJSONArray("memories")
                val existing = memoryService.getAllMemories().map { it.content }.toSet()
                for (i in 0 until memoriesArray.length()) {
                    val mObj = memoriesArray.getJSONObject(i)
                    val content = mObj.optString("content")
                    val type = mObj.optString("type", "permanent")
                    if (content.isNotBlank() && !existing.contains(content)) {
                        memoryService.saveMemory(content, type)
                    }
                }
            }

            // Restore Preferences
            if (root.has("preferences")) {
                val prefsObj = root.getJSONObject("preferences")
                val repo = prefsObj.optString("github_repo")
                if (repo.isNotBlank()) {
                    updateManager.setGitHubRepo(repo)
                }
            }

            return true
        } catch (_: Exception) {
            return false
        }
    }
}
