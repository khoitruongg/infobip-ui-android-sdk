package com.infobip.uisdk.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object InfobipTokenClient {
    private const val TOKEN_PATH = "/webrtc/1/token"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchToken(identity: String, displayName: String): String = withContext(Dispatchers.IO) {
        require(identity.isNotBlank()) { "Subscriber identity must not be blank" }
        require(BuildConfig.INFOBIP_API_KEY.isNotBlank()) {
            "API_KEY is missing. Add it to the project .env file."
        }

        val requestBody = JSONObject()
            .put("identity", identity.trim())
            .put("displayName", displayName.trim())
            .toString()

        val request = Request.Builder()
            .url("${normalizedBaseUrl()}$TOKEN_PATH")
            .addHeader("Authorization", "App ${BuildConfig.INFOBIP_API_KEY}")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: UnknownHostException) {
            val host = request.url.host
            throw IOException(
                "Infobip token request failed: unable to resolve host '$host'. " +
                    "Check network connectivity (emulator/device), VPN/Private DNS/proxy, " +
                    "and verify API_BASE_URL in .env (current: '${BuildConfig.INFOBIP_API_BASE_URL}').",
                e,
            )
        } catch (e: IOException) {
            throw IOException(
                "Infobip token request failed (${request.method} ${request.url}): ${e.message}",
                e,
            )
        }

        response.use {
            val responseBody = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                error("Infobip token request failed (${it.code}): $responseBody")
            }
            JSONObject(responseBody).getString("token")
        }
    }

    private fun normalizedBaseUrl(): String {
        val trimmed = BuildConfig.INFOBIP_API_BASE_URL.trim().removeSuffix("/")
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
