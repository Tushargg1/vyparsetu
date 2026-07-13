package com.vyaparsetu.notification.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyaparsetu.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Real WhatsApp sender backed by the Meta (WhatsApp) Cloud API.
 * Active only when {@code app.features.whatsapp.provider=meta}; marked primary so
 * it takes precedence over {@link NoopWhatsAppClient} when configured.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.features.whatsapp.provider", havingValue = "meta")
public class MetaWhatsAppClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppClient.class);

    private final AppProperties props;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public MetaWhatsAppClient(AppProperties props) {
        this.props = props;
    }

    private AppProperties.WhatsappFeature cfg() {
        return props.getFeatures().getWhatsapp();
    }

    @Override
    public boolean isEnabled() {
        AppProperties.WhatsappFeature c = cfg();
        return c.isEnabled() && c.getToken() != null && c.getPhoneNumberId() != null;
    }

    @Override
    public void sendText(String toPhone, String message) {
        if (!isEnabled()) return;
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "messaging_product", "whatsapp",
                    "to", toPhone,
                    "type", "text",
                    "text", Map.of("preview_url", false, "body", message)
            ));
            post(body);
        } catch (Exception e) {
            log.error("[WHATSAPP-META] send failed to {}: {}", toPhone, e.getMessage());
        }
    }

    @Override
    public void sendDocument(String toPhone, String caption, String documentUrl) {
        if (!isEnabled()) return;
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "messaging_product", "whatsapp",
                    "to", toPhone,
                    "type", "document",
                    "document", Map.of("link", documentUrl, "caption", caption == null ? "" : caption)
            ));
            post(body);
        } catch (Exception e) {
            log.error("[WHATSAPP-META] document send failed to {}: {}", toPhone, e.getMessage());
        }
    }

    private void post(String json) throws Exception {
        AppProperties.WhatsappFeature c = cfg();
        URI uri = URI.create(c.getApiUrl() + "/" + c.getPhoneNumberId() + "/messages");
        HttpRequest req = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + c.getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            log.error("[WHATSAPP-META] API {} -> {}", res.statusCode(), res.body());
        }
    }
}
