package com.vyaparsetu.ai.service;

/**
 * Abstraction over an LLM/AI provider. The default implementation is rule-based
 * so the platform works without external API keys; a Spring AI / OpenAI backed
 * implementation can be swapped in for production.
 */
public interface AiClient {
    String complete(String prompt);

    /** Whether a real LLM is configured (false for the rule-based fallback). */
    default boolean isAvailable() {
        return false;
    }
}
