package com.domedemok.travelplanner.data.remote

import com.domedemok.travelplanner.BuildKonfig
import com.domedemok.travelplanner.data.model.GeminiContent
import com.domedemok.travelplanner.data.model.GeminiPart
import com.domedemok.travelplanner.data.model.GeminiRequest
import com.domedemok.travelplanner.data.model.GeminiResponse
import com.domedemok.travelplanner.data.model.GeminiSystemInstruction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Wraps the Google Gemini `generateContent` REST endpoint and applies the
 * app's system prompt for the in-trip AI travel assistant.
 *
 * The reply is returned as a [Result] so callers can show specific error
 * messages (network down, key invalid, rate-limited) instead of a generic
 * "AI failed".  Successful responses are guaranteed non-null but may be empty
 * if Gemini returned no text candidate at all — treated as a failure.
 */
class AiService(
    private val httpClient: HttpClient,
) {

    private companion object {
        const val API_URL = "https://generativelanguage.googleapis.com/v1beta/" +
                            "models/gemini-3-flash-preview:generateContent"

        // The system prompt is intentionally strict: Gemini happily wanders into
        // markdown formatting and offers to take actions it cannot perform, both
        // of which break our rendering and confuse users.
        val SYSTEM_PROMPT = """
            You are a professional travel assistant.

            STRICT RULES FOR YOUR RESPONSES:
            1. PLAIN TEXT ONLY: DO NOT use any Markdown formatting whatsoever.
               No asterisks (* or **), no hashes (#) for headings. Use plain
               paragraphs separated by newlines.
            2. NO SYSTEM ACCESS: You are a read-only advisory chatbot. You DO
               NOT have access to the app's database, you cannot save places,
               and you cannot modify the user's itinerary.
            3. NO FALSE OFFERS: Never offer to perform actions for the user.
               Do not end messages with prompts like "Would you like me to add
               this to your planned places?" — just give helpful suggestions.
        """.trimIndent()
    }

    private val apiKey = BuildKonfig.GEMINI_API_KEY

    /**
     * Sends a single user turn to Gemini and returns the model's reply.
     *
     * [chatHistory] is the prior exchange in chronological order (user/model
     * alternating). [travelContext] is interpolated into the system prompt so
     * the model knows the trip details without us having to re-send them as
     * part of every user message.
     */
    suspend fun sendMessage(
        userMessage:   String,
        travelContext: String,
        chatHistory:   List<GeminiContent>,
    ): Result<String> = runCatching {
        val systemInstruction = GeminiSystemInstruction(
            parts = listOf(GeminiPart("$SYSTEM_PROMPT\n\nThe user's trip details: $travelContext")),
        )

        val request = GeminiRequest(
            systemInstruction = systemInstruction,
            contents = chatHistory + GeminiContent(
                role  = "user",
                parts = listOf(GeminiPart(userMessage)),
            ),
        )

        val response: GeminiResponse = httpClient.post("$API_URL?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Gemini returned no text candidate")
    }.onFailure { println("[AiService] sendMessage failed: ${it.message}") }
}
