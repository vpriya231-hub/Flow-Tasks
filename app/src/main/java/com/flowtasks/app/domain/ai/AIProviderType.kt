package com.flowtasks.app.domain.ai

/**
 * Supported and extensible AI Provider types for Flow Tasks.
 * This foundation is provider-agnostic, allowing seamless support for Gemini,
 * and future integration of OpenAI, Anthropic, or custom providers.
 */
enum class AIProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    CUSTOM("Custom Endpoint");

    companion object {
        fun fromString(value: String): AIProviderType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GEMINI
        }
    }
}
