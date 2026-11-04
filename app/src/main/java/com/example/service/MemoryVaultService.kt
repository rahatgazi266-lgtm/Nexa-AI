package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MemoryItem
import org.json.JSONArray
import org.json.JSONObject

class MemoryVaultService(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexa_memory_prefs", Context.MODE_PRIVATE)

    private val KEY_MEMORIES = "vault_memories"

    fun getAllMemories(): List<MemoryItem> {
        val jsonStr = prefs.getString(KEY_MEMORIES, "[]") ?: "[]"
        val list = mutableListOf<MemoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MemoryItem(
                        id = obj.optString("id"),
                        content = obj.optString("content"),
                        type = obj.optString("type", "permanent"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveMemory(content: String, type: String = "permanent"): MemoryItem {
        val memories = getAllMemories().toMutableList()
        val item = MemoryItem(
            content = content.trim(),
            type = type,
            timestamp = System.currentTimeMillis()
        )
        memories.add(0, item)
        saveList(memories)
        return item
    }

    fun deleteMemory(id: String) {
        val memories = getAllMemories().filterNot { it.id == id }
        saveList(memories)
    }

    fun clearTemporaryMemories() {
        val memories = getAllMemories().filter { it.type == "permanent" }
        saveList(memories)
    }

    fun clearAllMemories() {
        saveList(emptyList())
    }

    fun tryAutoCapture(userMessage: String) {
        val msg = userMessage.trim()
        val patterns = listOf(
            Regex("my name is (.+)", RegexOption.IGNORE_CASE),
            Regex("i like (.+)", RegexOption.IGNORE_CASE),
            Regex("remember that (.+)", RegexOption.IGNORE_CASE),
            Regex("আমার নাম (.+)", RegexOption.IGNORE_CASE),
            Regex("আমি (.+) পছন্দ করি", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            if (p.containsMatchIn(msg)) {
                saveMemory(msg, "permanent")
                break
            }
        }
    }

    private fun saveList(items: List<MemoryItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("content", item.content)
            obj.put("type", item.type)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(KEY_MEMORIES, array.toString()).apply()
    }
}
