package com.vyaparsetu.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Resilient LLM entry point. Tries providers in order and fails over on any
 * error (rate limit / quota / outage):
 * <p>1) Groq primary model (Llama 3.3) → 2) Groq fallback model (Llama 3.1)
 * → 3) Gemini Flash. The app keeps working as long as one provider responds.
 */
@Component
@Primary
public class AiProviderChain implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiProviderChain.class);
    private static final String DEFAULT_SYSTEM =
            "You are a helpful assistant for an Indian B2B distribution app (VyaparMantra). "
                    + "Be concise and practical. Reply in the user's language (Hindi/English).";

    private final GroqAiClient groq;
    private final GeminiAiClient gemini;

    public AiProviderChain(GroqAiClient groq, GeminiAiClient gemini) {
        this.groq = groq;
        this.gemini = gemini;
    }

    @Override
    public boolean isAvailable() {
        return groq.isAvailable() || gemini.isAvailable();
    }

    @Override
    public String complete(String prompt) {
        if (!isAvailable()) {
            return "AI assistant (offline mode). Configure an API key to enable full responses.";
        }
        String out = generate(DEFAULT_SYSTEM, prompt, 0.4);
        return out != null ? out : "Sorry, the assistant is temporarily unavailable. Please try again.";
    }

    /**
     * Run the prompt through the provider chain. Returns the first successful
     * response, or null if every provider failed.
     */
    public String generate(String system, String user, double temperature) {
        List<Attempt> chain = new ArrayList<>();
        if (groq.isAvailable()) {
            chain.add(new Attempt("groq:" + groq.primaryModel(),
                    () -> groq.chat(system, user, temperature, groq.primaryModel())));
            if (groq.fallbackModel() != null && !groq.fallbackModel().isBlank()) {
                chain.add(new Attempt("groq:" + groq.fallbackModel(),
                        () -> groq.chat(system, user, temperature, groq.fallbackModel())));
            }
        }
        if (gemini.isAvailable()) {
            chain.add(new Attempt("gemini:" + gemini.model(),
                    () -> gemini.generate(system, user, temperature)));
        }

        for (Attempt a : chain) {
            try {
                String res = a.call.run();
                if (res != null && !res.isBlank()) {
                    return res;
                }
                log.warn("[AI] {} returned empty, trying next provider", a.name);
            } catch (Exception e) {
                log.warn("[AI] {} failed ({}), trying next provider", a.name, e.getMessage());
            }
        }
        log.error("[AI] all providers failed");
        return null;
    }

    private record Attempt(String name, Call call) {
    }

    @FunctionalInterface
    private interface Call {
        String run() throws Exception;
    }
}
