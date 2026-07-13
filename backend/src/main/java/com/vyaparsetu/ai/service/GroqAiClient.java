package com.vyaparsetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyaparsetu.common.config.AppProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Low-level Groq client (OpenAI-compatible Chat Completions). Used by the
 * {@link AiProviderChain}; supports calling a specific model so the chain can
 * fail over from the primary model to a lighter fallback model.
 */
@Component
public class GroqAiClient {

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqAiClient(AppProperties props) {
        this.props = props;
    }

    private AppProperties.Ai.Groq cfg() {
        return props.getAi().getGroq();
    }

    public boolean isAvailable() {
        String key = cfg().getApiKey();
        return key != null && !key.isBlank();
    }

    public String primaryModel() {
        return cfg().getModel();
    }

    public String fallbackModel() {
        return cfg().getFallbackModel();
    }

    /** Call a specific Groq model. Throws on any non-2xx (e.g. 429 quota) so the chain can fail over. */
    public String chat(String system, String user, double temperature, String model) throws Exception {
        AppProperties.Ai.Groq c = cfg();
        String body = mapper.writeValueAsString(Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                )
        ));
        HttpRequest req = HttpRequest.newBuilder(URI.create(c.getBaseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + c.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            throw new RuntimeException("Groq " + model + " HTTP " + res.statusCode() + ": " + res.body());
        }
        JsonNode root = mapper.readTree(res.body());
        return root.path("choices").path(0).path("message").path("content").asText("");
    }
}
