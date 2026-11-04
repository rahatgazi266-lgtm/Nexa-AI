package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ChatMessage
import com.example.model.ChatSession
import org.json.JSONArray
import org.json.JSONObject

class ChatSessionService(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexa_sessions_prefs", Context.MODE_PRIVATE)

    private val KEY_SESSIONS = "chat_sessions"

    fun getAllSessions(): List<ChatSession> {
        val jsonStr = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val list = mutableListOf<ChatSession>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val msgsArray = obj.optJSONArray("messages") ?: JSONArray()
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
                list.add(
                    ChatSession(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        date = obj.optString("date"),
                        messages = msgs
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveSession(session: ChatSession) {
        val sessions = getAllSessions().filterNot { it.id == session.id }.toMutableList()
        sessions.add(0, session)
        val array = JSONArray()
        for (s in sessions.take(30)) {
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
            array.put(sObj)
        }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun deleteSession(sessionId: String) {
        val sessions = getAllSessions().filterNot { it.id == sessionId }
        val array = JSONArray()
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
                msgsArray.put(mObj)
            }
            sObj.put("messages", msgsArray)
            array.put(sObj)
        }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }
}
