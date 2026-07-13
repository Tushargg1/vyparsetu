package com.vyaparsetu.whatsapp.service;

import com.vyaparsetu.whatsapp.entity.WhatsAppMessage;
import com.vyaparsetu.whatsapp.repository.WhatsAppMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores WhatsApp conversation history and provides webhook idempotency
 * (duplicate provider message ids are processed once).
 */
@Service
public class ConversationLogService {

    private static final Logger log = LoggerFactory.getLogger(ConversationLogService.class);

    private final WhatsAppMessageRepository repo;

    public ConversationLogService(WhatsAppMessageRepository repo) {
        this.repo = repo;
    }

    /** True if this provider message id was already processed (duplicate webhook). */
    @Transactional(readOnly = true)
    public boolean isDuplicate(String providerMessageId) {
        return providerMessageId != null && repo.existsByProviderMessageId(providerMessageId);
    }

    @Transactional
    public void logInbound(Long supplierId, String phone, String body, String providerMessageId) {
        save(supplierId, phone, "IN", body, providerMessageId);
        log.info("[WA][IN] supplier={} phone={} msg={}", supplierId, mask(phone), preview(body));
    }

    @Transactional
    public void logOutbound(Long supplierId, String phone, String body) {
        if (body == null || body.isBlank()) return;
        save(supplierId, phone, "OUT", body, null);
    }

    private void save(Long supplierId, String phone, String direction, String body, String providerMessageId) {
        try {
            WhatsAppMessage m = new WhatsAppMessage();
            m.setSupplierId(supplierId);
            m.setPhone(phone);
            m.setDirection(direction);
            m.setBody(body != null && body.length() > 3900 ? body.substring(0, 3900) : body);
            m.setProviderMessageId(providerMessageId);
            repo.save(m);
        } catch (Exception e) {
            // never let logging break the conversation
            log.warn("[WA] message log failed: {}", e.getMessage());
        }
    }

    /** Recent messages (oldest→newest) for building AI context. */
    @Transactional(readOnly = true)
    public List<String> recentContext(Long supplierId, String phone, int limit) {
        List<WhatsAppMessage> rows = repo.findBySupplierIdAndPhoneOrderByIdDesc(
                supplierId, phone, PageRequest.of(0, limit));
        List<String> out = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            WhatsAppMessage m = rows.get(i);
            out.add(("IN".equals(m.getDirection()) ? "Customer: " : "Assistant: ") + m.getBody());
        }
        return out;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private String preview(String body) {
        if (body == null) return "";
        return body.length() > 60 ? body.substring(0, 60) + "…" : body;
    }
}
