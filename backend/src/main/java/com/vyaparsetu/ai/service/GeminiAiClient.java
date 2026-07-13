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
 * Low-level Google Gemini client (generateContent). Used by the
 * {@link AiProviderChain} as the final fallback and for harder inputs.
 */
@Component
public class GeminiAiClient {

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiAiClient(AppProperties props) {
        this.props = props;
    }

    private AppProperties.Ai.Gemini cfg() {
        return props.getAi().getGemini();
    }

    public boolean isAvailable() {
        String key = cfg().getApiKey();
        // Google AI Studio keys start with "AIza". Anything else (e.g. an OAuth
        // access token) will not work with the generateContent API, so skip it
        // to avoid wasted failover attempts.
        return key != null && key.startsWith("AIza");
    }

    public String model() {
        return cfg().getModel();
    }

    /** Call Gemini. Throws on any non-2xx so the chain can fail over. */
    public String generate(String system, String user, double temperature) throws Exception {
        AppProperties.Ai.Gemini c = cfg();
        String prompt = (system == null ? "" : system + "\n\n") + user;
        String body = mapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", temperature)
        ));
        URI uri = URI.create(c.getBaseUrl() + "/models/" + c.getModel() + ":generateContent?key=" + c.getApiKey());
        HttpRequest req = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            throw new RuntimeException("Gemini HTTP " + res.statusCode() + ": " + res.body());
        }
        JsonNode root = mapper.readTree(res.body());
        return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
    }
}
