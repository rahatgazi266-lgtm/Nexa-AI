package com.example.service

import android.content.Context
import com.example.BuildConfig
import com.example.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class NexaBrainService(private val context: Context) {
    companion object {
        const val CREATOR_NAME = "MD Rahat Gazi"
        const val PREFS_NAME = "nexa_settings_prefs"
        const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        const val EXACT_MODEL_ID = "gemini-3.5-flash-lite"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val keyStoreManager = SecureKeyStoreManager(context)

    fun getSavedApiKey(): String {
        return keyStoreManager.getApiKey()
    }

    fun saveApiKey(key: String) {
        keyStoreManager.storeApiKey(key)
    }

    fun removeApiKey() {
        keyStoreManager.removeApiKey()
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        memories: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val q = prompt.trim()
        val lower = q.lowercase()

        // 1. Strict Creator Attribution Lock
        val creatorQueries = listOf(
            "who created you", "who made you", "who is your creator",
            "creator ke", "who built you", "who is your owner", "owner",
            "tomake ke baniyeche", "tomake ke banise", "কার তৈরি", "তৈরি করেছে",
            "কে বানিয়েছে", "কে তৈরি করেছেন", "who is your developer", "developer ke"
        )
        if (creatorQueries.any { lower.contains(it) }) {
            return@withContext "I was created by $CREATOR_NAME."
        }

        val apiKey = getSavedApiKey()

        // 2. Direct Google Gemini call if API Key is configured
        if (apiKey.isNotBlank()) {
            return@withContext executeGeminiCall(apiKey, q, history, memories)
        }

        // 3. If no API key is provided, handle basic greetings or provide clear setup instruction
        handleMissingApiKey(q)
    }

    private fun executeGeminiCall(
        apiKey: String,
        prompt: String,
        history: List<ChatMessage>,
        memories: List<String>
    ): String {
        return try {
            val result = callGeminiEndpoint(apiKey, EXACT_MODEL_ID, prompt, history, memories)
            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                result.exceptionOrNull()?.message ?: "⚠️ **Gemini API Error:** Could not complete request with Gemini 3.5 Flash-Lite."
            }
        } catch (e: Exception) {
            formatException(e)
        }
    }

    private fun callGeminiEndpoint(
        apiKey: String,
        modelName: String,
        prompt: String,
        history: List<ChatMessage>,
        memories: List<String>
    ): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Normalize prior history so turns alternate strictly between "user" and "model"
        // Exclude the current prompt if it was already added to history
        val priorHistory = if (history.isNotEmpty() && history.last().isUser && history.last().text.trim() == prompt.trim()) {
            history.dropLast(1)
        } else {
            history
        }

        val normalizedTurns = mutableListOf<Pair<String, String>>()
        for (msg in priorHistory.takeLast(12)) {
            val text = msg.text.trim()
            if (text.isBlank()) continue
            if (!msg.isUser && text.startsWith("⚠️")) continue
            val role = if (msg.isUser) "user" else "model"

            if (normalizedTurns.isNotEmpty() && normalizedTurns.last().first == role) {
                // Merge consecutive same-role turns to prevent Gemini HTTP 400
                val previous = normalizedTurns.removeAt(normalizedTurns.size - 1)
                normalizedTurns.add(Pair(role, previous.second + "\n\n" + text))
            } else {
                normalizedTurns.add(Pair(role, text))
            }
        }

        // Gemini conversation must start with a user turn
        while (normalizedTurns.isNotEmpty() && normalizedTurns.first().first != "user") {
            normalizedTurns.removeAt(0)
        }

        // If the last turn in normalized history is already a "user" turn, merge current prompt into it
        val promptWithContext = if (memories.isNotEmpty()) {
            val memText = memories.take(5).joinToString("\n") { "- $it" }
            "[Nexa Saved Memory Context:\n$memText\n]\n\nUser Question:\n$prompt"
        } else {
            prompt
        }

        if (normalizedTurns.isNotEmpty() && normalizedTurns.last().first == "user") {
            val lastUser = normalizedTurns.removeAt(normalizedTurns.size - 1)
            normalizedTurns.add(Pair("user", lastUser.second + "\n\n" + promptWithContext))
        } else {
            normalizedTurns.add(Pair("user", promptWithContext))
        }

        // Build contents JSON
        for (turn in normalizedTurns) {
            val turnObj = JSONObject()
            turnObj.put("role", turn.first)
            val parts = JSONArray()
            parts.put(JSONObject().put("text", turn.second))
            turnObj.put("parts", parts)
            contentsArray.put(turnObj)
        }

        val requestBodyJson = JSONObject()
        requestBodyJson.put("contents", contentsArray)

        // System Instruction
        val systemInstruction = "You are Nexa AI, a personal AI assistant created by $CREATOR_NAME. " +
                "Always communicate fluently, naturally, and intelligently in the user's language (Bengali, English, Hindi, Arabic, etc.). " +
                "Deeply understand the meaning of each user question and provide realistic, factual, relevant, and well-structured answers using clean markdown. " +
                "If asked who created you, who made you, or who is your developer, always state: 'I was created by $CREATOR_NAME'."

        val systemObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemInstruction))
        systemObj.put("parts", sysParts)
        requestBodyJson.put("systemInstruction", systemObj)

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("maxOutputTokens", 2048)
        requestBodyJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        if (response.isSuccessful) {
            try {
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "").trim()
                        if (text.isNotBlank()) {
                            return Result.success(text)
                        }
                    }
                }
                return Result.failure(Exception("⚠️ **Gemini API:** Empty response returned by model."))
            } catch (e: Exception) {
                return Result.failure(Exception("⚠️ **JSON Parse Error:** Could not parse Gemini response: ${e.message}"))
            }
        }

        // Handle specific API errors
        val parsedError = parseApiError(bodyStr, response.code)
        return Result.failure(Exception(parsedError))
    }

    private fun parseApiError(responseBody: String, statusCode: Int): String {
        var rawMessage = ""
        var errorStatus = ""
        try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                rawMessage = errObj.optString("message", "")
                errorStatus = errObj.optString("status", "")
            }
        } catch (_: Exception) {}

        return when (statusCode) {
            400 -> {
                if (rawMessage.contains("API key not valid", ignoreCase = true)) {
                    "⚠️ **Gemini API ত্রুটি (Invalid API Key - 400):**\n" +
                            "আপনার প্রদান করা API Key-টি সঠিক নয়।\n\n" +
                            "🔧 **সমাধান:**\n" +
                            "- মেনু থেকে **Settings & API Key** খুলুন।\n" +
                            "- [Google AI Studio](https://aistudio.google.com/app/apikey) থেকে প্রাপ্ত আপনার সঠিক Gemini API Key (AIzaSy...) পেস্ট করে **Save Key** বাটনে চাপুন।"
                } else {
                    "⚠️ **Gemini API ত্রুটি (HTTP 400 - Bad Request):**\n${rawMessage.ifEmpty { "অনুরোধটি সঠিক নয়।" }}"
                }
            }
            401 -> {
                "⚠️ **Gemini API ত্রুটি (HTTP 401 - Unauthorized):**\n" +
                        "API Key অননুমোদিত। অনুগ্রহ করে Settings থেকে একটি সক্রিয় ও বৈধ Gemini API Key প্রদান করুন।"
            }
            403 -> {
                "⚠️ **Gemini API ত্রুটি (HTTP 403 - Permission Denied):**\n" +
                        "${rawMessage.ifEmpty { "এই API Key-এর জন্য Generative Language API ব্যবহারের অনুমতি নেই।" }}\n\n" +
                        "অনুগ্রহ করে Google AI Studio-তে আপনার API Key-এর স্ট্যাটাস যাচাই করুন।"
            }
            429 -> {
                "⚠️ **Gemini API কোটা সীমা অতিক্রম (HTTP 429 - Rate Limit):**\n" +
                        "Google Gemini Free Tier-এর কোটা সীমা (15 RPM / 500 RPD) সাময়িকভাবে শেষ হয়েছে।\n" +
                        "অনুগ্রহ করে ১-২ মিনিট অপেক্ষা করে পুনরায় প্রশ্ন করুন।"
            }
            500, 502, 503, 504 -> {
                "⚠️ **Google Server ত্রুটি (HTTP $statusCode):**\n" +
                        "Google Gemini সার্ভার সাময়িকভাবে ব্যস্ত। অনুগ্রহ করে কয়েক সেকেন্ড পর আবার চেষ্টা করুন।"
            }
            else -> {
                val detail = if (rawMessage.isNotBlank()) "\nবার্তা: $rawMessage" else ""
                "⚠️ **Gemini API ত্রুটি (HTTP $statusCode $errorStatus):**$detail"
            }
        }
    }

    private fun formatException(e: Exception): String {
        return when (e) {
            is UnknownHostException -> {
                "⚠️ **ইন্টারনেট সংযোগ ত্রুটি (Network Error):**\n" +
                        "Google Gemini সার্ভারের সাথে সংযোগ স্থাপন করা যাচ্ছে না। অনুগ্রহ করে আপনার মোবাইল ইন্টারনেট বা Wi-Fi সংযোগ সক্রিয় রয়েছে কিনা পরীক্ষা করুন।"
            }
            is SocketTimeoutException -> {
                "⚠️ **টাইমআউট ত্রুটি (Connection Timeout):**\n" +
                        "Google Gemini থেকে উত্তর আসতে অতিরিক্ত সময় লেগেছে। অনুগ্রহ করে ইন্টারনেট স্পিড চেক করে পুনরায় চেষ্টা করুন।"
            }
            is IOException -> {
                "⚠️ **নেটওয়ার্ক ত্রুটি (IO Error):**\n" +
                        "${e.localizedMessage ?: "সার্ভারের সাথে যোগাযোগের সময় ত্রুটি ঘটেছে।"}"
            }
            else -> {
                "⚠️ **ত্রুটি:** ${e.localizedMessage ?: e.message ?: "অপ্রত্যাশিত সমস্যা দেখা দিয়েছে।"}"
            }
        }
    }

    private fun handleMissingApiKey(prompt: String): String {
        val q = prompt.lowercase().trim()

        // Natural Bengali & English basic responses when key is not yet set
        if (q.contains("hello") || q.contains("hi") || q.contains("নমস্কার") || q.contains("সালাম") || q.contains("কেমন আছো")) {
            return "Hello! আমি **Nexa AI**, **$CREATOR_NAME**-এর তৈরি আপনার পার্সোনাল এআই অ্যাসিস্ট্যান্ট।\n\n" +
                    "আমি আপনাকে বিভিন্ন বিষয়ে সাহায্য করতে প্রস্তুত।\n\n" +
                    "💡 **জরুরি নির্দেশনা:**\n" +
                    "Nexa AI-এর সম্পূর্ণ Gemini Brain এবং যেকোনো মুক্ত প্রশ্নের বাস্তবসম্মত উত্তর পেতে অনুগ্রহ করে **Settings & API Key**-এ গিয়ে আপনার Google Gemini API Key যুক্ত করুন।"
        }

        if (q.contains("who are you") || q.contains("তুমি কে") || q.contains("about nexa") || q.contains("your name")) {
            return "**Nexa AI** হলো **$CREATOR_NAME** কর্তৃক নির্মিত একটি আধুনিক পার্সোনাল এআই অ্যাসিস্ট্যান্ট।\n\n" +
                    "**প্রধান বৈশিষ্ট্যসমূহ:**\n" +
                    "- 🧠 **Intelligent Memory Vault**: কথোপকথন ও গুরুত্বপূর্ণ তথ্য ডিভাইসে মনে রাখে\n" +
                    "- 🛡️ **Security Shield**: ক্ষতিকর লিংক, স্ক্রিপ্ট ও ফাইল রিয়েল-টাইমে প্রতিরোধ করে\n" +
                    "- ⚡ **Ocean Core Engine**: অত্যন্ত দ্রুত ও শক্তিশালী আর্কিটেকচার\n" +
                    "- 🌐 **বহুভাষিক সাপোর্ট**: বাংলা, ইংরেজি ইত্যাদিতে স্বাভাবিক কথোপকথন"
        }

        // For any specific question without an API key: explain clearly instead of repeating canned defaults
        return "⚠️ **Gemini API Key সংযুক্ত করা নেই**\n\n" +
                "আপনি জানতে চেয়েছেন: *\"$prompt\"*\n\n" +
                "এই প্রশ্নের বাস্তবসম্মত, প্রাসঙ্গিক ও গভীর বিশ্লেষণমূলক উত্তর তৈরি করতে Nexa AI-এর **Google Gemini AI Brain** সক্রিয় করা প্রয়োজন।\n\n" +
                "🔧 **মাত্র ১ মিনিটে যেভাবে চালু করবেন:**\n" +
                "1. উপরের বাঁপাশের মেনু থেকে **Settings & API Key** অপশনটি খুলুন।\n" +
                "2. আপনার Google Gemini API Key (AIzaSy...) পেস্ট করে **Save Key** বাটনে চাপুন।\n" +
                "3. সম্পূর্ণ বিনামূল্যে API Key পেতে ভিজিট করুন: [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)\n\n" +
                "*Key যুক্ত করার সাথে সাথেই Nexa AI যেকোনো প্রশ্নের তাৎক্ষণিক ও সঠিক উত্তর দিতে শুরু করবে।*"
    }
}
