package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class MusicGenerationResult(
    val audioFile: File,
    val mimeType: String,
    val lyricsOrStructure: String,
    val modelUsed: String
)

data class VideoGenerationResult(
    val videoFile: File,
    val aspectRatio: String,
    val interactionId: String
)

class InteractionsMediaClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/interactions"

    private fun requireApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "GEMINI_API_KEY is not configured. Open the Secrets panel in AI Studio to add your Gemini API key."
            )
        }
        return key
    }

    suspend fun generateMusic(
        prompt: String,
        modelId: String
    ): Result<MusicGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireApiKey()
            val allowedModel = if (modelId == "lyria-3-pro-preview") {
                "lyria-3-pro-preview"
            } else {
                "lyria-3-clip-preview"
            }

            val payload = JSONObject().apply {
                put("model", allowedModel)
                put("input", prompt.trim())
            }

            val initialJson = postInteraction(apiKey, payload)
            val completedJson = pollUntilCompleteIfNeeded(apiKey, initialJson, maxAttempts = 45)

            val audioExtraction = extractMedia(completedJson, targetKind = "audio")
                ?: throw IllegalStateException(
                    "No audio data returned from $allowedModel. Response status: ${completedJson.optString("status")}"
                )

            val bytes = resolveMediaBytes(apiKey, audioExtraction)
            val ext = if (audioExtraction.mimeType.contains("wav")) "wav" else "mp3"
            val outFile = File(context.cacheDir, "lyria_${System.currentTimeMillis()}.$ext")
            outFile.writeBytes(bytes)

            val notes = extractText(completedJson)
            Result.success(
                MusicGenerationResult(
                    audioFile = outFile,
                    mimeType = audioExtraction.mimeType,
                    lyricsOrStructure = notes,
                    modelUsed = allowedModel
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Music generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun animateImageToVideo(
        imageUri: Uri,
        prompt: String,
        aspectRatio: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<VideoGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = requireApiKey()
            val safeAspectRatio = if (aspectRatio == "9:16") "9:16" else "16:9"

            onStatusUpdate("Encoding photo for Gemini Omni 1.2 Flash...")
            val jpegBytes = readAndCompressPhoto(imageUri)
            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val inputArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "image")
                    put("data", base64Image)
                    put("mime_type", "image/jpeg")
                })
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt.trim())
                })
            }

            val responseFormat = JSONObject().apply {
                put("type", "video")
                put("aspect_ratio", safeAspectRatio)
                put("resolution", "720p")
                put("duration", "5s")
            }

            val payload = JSONObject().apply {
                put("model", "gemini-omni-1.2-flash")
                put("input", inputArray)
                put("background", true)
                put("store", true)
                put("response_format", responseFormat)
            }

            onStatusUpdate("Submitting video job via Interactions API...")
            val initialJson = postInteraction(apiKey, payload)
            val interactionId = initialJson.optString("id", "")

            onStatusUpdate("Rendering $safeAspectRatio cinematic video...")
            val completedJson = pollUntilCompleteIfNeeded(
                apiKey = apiKey,
                initial = initialJson,
                maxAttempts = 60,
                onPoll = { attempt ->
                    onStatusUpdate("Generating $safeAspectRatio video (step $attempt)...")
                }
            )

            val videoExtraction = extractMedia(completedJson, targetKind = "video")
                ?: throw IllegalStateException(
                    "No video output returned from gemini-omni-1.2-flash (status=${completedJson.optString("status")})."
                )

            onStatusUpdate("Downloading generated MP4...")
            val videoBytes = resolveMediaBytes(apiKey, videoExtraction)
            val outFile = File(context.cacheDir, "omni_video_${System.currentTimeMillis()}.mp4")
            outFile.writeBytes(videoBytes)

            Result.success(
                VideoGenerationResult(
                    videoFile = outFile,
                    aspectRatio = safeAspectRatio,
                    interactionId = interactionId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Video generation failed", e)
            Result.failure(e)
        }
    }

    private fun readAndCompressPhoto(uri: Uri): ByteArray {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not open selected image.")
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 1080) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: throw IllegalArgumentException("Invalid image format.")
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun postInteraction(apiKey: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(parseApiError(response.code, responseText))
            }
            return JSONObject(responseText)
        }
    }

    private fun getInteraction(apiKey: String, interactionId: String): JSONObject {
        val request = Request.Builder()
            .url("$baseUrl/$interactionId")
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(parseApiError(response.code, responseText))
            }
            return JSONObject(responseText)
        }
    }

    private suspend fun pollUntilCompleteIfNeeded(
        apiKey: String,
        initial: JSONObject,
        maxAttempts: Int,
        onPoll: (Int) -> Unit = {}
    ): JSONObject {
        var current = initial
        val id = current.optString("id", "")
        var attempt = 0
        while (attempt < maxAttempts) {
            val status = current.optString("status", "").lowercase()
            if (status == "failed" || status == "cancelled" || status == "error") {
                val errMsg = current.optJSONObject("error")?.optString("message")
                    ?: "Interaction failed with status: $status"
                throw IllegalStateException(errMsg)
            }
            val alreadyHasOutput =
                current.has("outputs") && (current.optJSONArray("outputs")?.length() ?: 0) > 0 ||
                    current.has("output_audio") ||
                    current.has("output_video")
            if (status == "completed" || status == "succeeded" || (status.isEmpty() && alreadyHasOutput)) {
                return current
            }
            if (id.isBlank()) {
                return current
            }
            attempt++
            onPoll(attempt)
            delay(3000L)
            current = getInteraction(apiKey, id)
        }
        return current
    }

    private data class ExtractedMedia(
        val base64Data: String?,
        val uri: String?,
        val mimeType: String
    )

    private fun extractMedia(json: JSONObject, targetKind: String): ExtractedMedia? {
        // 1. Convenience objects output_audio / output_video
        val convKey = if (targetKind == "audio") "output_audio" else "output_video"
        json.optJSONObject(convKey)?.let { obj ->
            val data = obj.optString("data", "").takeIf { it.isNotBlank() }
            val uri = obj.optString("uri", "").takeIf { it.isNotBlank() }
            val mime = obj.optString("mime_type", if (targetKind == "audio") "audio/mp3" else "video/mp4")
            if (data != null || uri != null) return ExtractedMedia(data, uri, mime)
        }

        // 2. Top-level outputs array
        json.optJSONArray("outputs")?.let { arr ->
            findMediaInContentArray(arr, targetKind)?.let { return it }
        }

        // 3. Steps[].content[]
        json.optJSONArray("steps")?.let { steps ->
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i) ?: continue
                step.optJSONArray("content")?.let { contentArr ->
                    findMediaInContentArray(contentArr, targetKind)?.let { return it }
                }
            }
        }
        return null
    }

    private fun findMediaInContentArray(arr: JSONArray, targetKind: String): ExtractedMedia? {
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val type = item.optString("type", "").lowercase()
            val mime = item.optString("mime_type", "")
            val matches = type == targetKind ||
                mime.startsWith("$targetKind/") ||
                (targetKind == "audio" && type.contains("audio")) ||
                (targetKind == "video" && type.contains("video"))
            if (matches) {
                val data = item.optString("data", "").takeIf { it.isNotBlank() }
                val uri = item.optString("uri", "").takeIf { it.isNotBlank() }
                if (data != null || uri != null) {
                    return ExtractedMedia(
                        base64Data = data,
                        uri = uri,
                        mimeType = mime.ifBlank { if (targetKind == "audio") "audio/mp3" else "video/mp4" }
                    )
                }
            }
        }
        return null
    }

    private fun extractText(json: JSONObject): String {
        val outputText = json.optString("output_text", "")
        if (outputText.isNotBlank()) return outputText
        val sb = StringBuilder()
        json.optJSONArray("outputs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optString("type") == "text") {
                    val t = item.optString("text", "")
                    if (t.isNotBlank()) sb.appendLine(t)
                }
            }
        }
        return sb.toString().trim()
    }

    private fun resolveMediaBytes(apiKey: String, media: ExtractedMedia): ByteArray {
        if (!media.base64Data.isNullOrBlank()) {
            return Base64.decode(media.base64Data, Base64.DEFAULT)
        }
        val uri = media.uri ?: throw IllegalStateException("Media URI and data are both empty")
        val req = Request.Builder()
            .url(uri)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Failed to download generated media (HTTP ${resp.code})")
            }
            return resp.body?.bytes() ?: throw IllegalStateException("Empty media download body")
        }
    }

    private fun parseApiError(code: Int, rawBody: String): String {
        return try {
            val errObj = JSONObject(rawBody).optJSONObject("error")
            val msg = errObj?.optString("message").orEmpty()
            if (msg.isNotBlank()) "API Error ($code): $msg" else "API Error ($code): $rawBody"
        } catch (_: Exception) {
            "API Error ($code): ${rawBody.take(240)}"
        }
    }

    companion object {
        private const val TAG = "InteractionsMediaClient"
    }
}
