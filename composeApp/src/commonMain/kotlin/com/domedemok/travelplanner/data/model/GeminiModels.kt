package com.domedemok.travelplanner.data.model

import kotlinx.serialization.Serializable

// Wire-format DTOs for the Google Gemini `generateContent` REST endpoint.
// See: https://ai.google.dev/api/generate-content

// ── Request ─────────────────────────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val contents:          List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiContent(
    val role:  String? = null,           // "user" | "model"
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String,
)

// ── Response ────────────────────────────────────────────────────────────────

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
)
