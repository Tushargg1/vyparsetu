package com.vyaparsetu.notification.whatsapp;

import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic inbound WhatsApp webhook. Accepts either the simple
 * {from, to, text} shape or a Meta Cloud API payload. The GET endpoint handles
 * Meta's webhook verification handshake.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp", description = "Inbound WhatsApp webhook (join/order/onboarding)")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService whatsAppService;
    private final AppProperties props;

    public WhatsAppWebhookController(WhatsAppService whatsAppService, AppProperties props) {
        this.whatsAppService = whatsAppService;
        this.props = props;
    }

    /** Meta webhook verification: echo hub.challenge when the verify token matches. */
    @GetMapping("/webhook")
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        String expected = props.getFeatures().getWhatsapp().getVerifyToken();
        if ("subscribe".equals(mode) && expected != null && expected.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    @PostMapping("/webhook")
    @Operation(summary = "Receive an inbound WhatsApp message (simple or Meta payload)")
    public ApiResponse<Map<String, String>> webhook(@RequestBody Map<String, Object> payload) {
        Inbound in = extract(payload);
        if (in == null) {
            return ApiResponse.ok(Map.of("reply", ""));
        }
        String reply = whatsAppService.handleInbound(in.from, in.to, in.text, in.id);
        return ApiResponse.ok(Map.of("reply", reply == null ? "" : reply));
    }

    private record Inbound(String from, String to, String text, String id) {
    }

    /** Map either the simple shape or a Meta Cloud API payload to (from, to, text, id). */
    @SuppressWarnings("unchecked")
    private Inbound extract(Map<String, Object> payload) {
        try {
            if (payload.containsKey("entry")) {
                List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
                for (Map<String, Object> entry : entries) {
                    List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                    if (changes == null) continue;
                    for (Map<String, Object> change : changes) {
                        Map<String, Object> value = (Map<String, Object>) change.get("value");
                        if (value == null) continue;
                        Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
                        String to = metadata != null ? str(metadata.get("display_phone_number")) : null;
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages == null || messages.isEmpty()) continue;
                        Map<String, Object> msg = messages.get(0);
                        String from = str(msg.get("from"));
                        String id = str(msg.get("id"));
                        Map<String, Object> text = (Map<String, Object>) msg.get("text");
                        String body = text != null ? str(text.get("body")) : null;
                        // Interactive (button/list) replies map to their selected id/title.
                        if (body == null) {
                            body = interactiveReply(msg);
                        }
                        if (from != null && body != null) {
                            return new Inbound(from, to, body, id);
                        }
                    }
                }
                return null;
            }
            // simple shape: {from, to, text, id}
            return new Inbound(str(payload.get("from")), str(payload.get("to")),
                    str(payload.get("text")), str(payload.get("id")));
        } catch (Exception e) {
            log.warn("[WHATSAPP] could not parse inbound payload: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String interactiveReply(Map<String, Object> msg) {
        Map<String, Object> interactive = (Map<String, Object>) msg.get("interactive");
        if (interactive == null) return null;
        Map<String, Object> br = (Map<String, Object>) interactive.get("button_reply");
        if (br != null) return str(br.get("title"));
        Map<String, Object> lr = (Map<String, Object>) interactive.get("list_reply");
        if (lr != null) return str(lr.get("title"));
        return null;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
