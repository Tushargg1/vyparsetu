package com.vyaparsetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyaparsetu.ai.dto.ParsedOrderResponse;
import com.vyaparsetu.ai.dto.TextOrderRequest;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a free-text shopping list into structured order lines by matching
 * against the supplier's catalog. Uses the LLM ({@link AiClient}) to extract
 * items/quantities from messy or Hinglish messages when available, and falls
 * back to a rule-based parser otherwise (so it always works offline).
 */
@Service
public class AiOrderService {

    private static final Logger log = LoggerFactory.getLogger(AiOrderService.class);
    private static final Pattern QTY = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final double CONFIDENCE_FLOOR = 0.85;

    private final ProductRepository productRepository;
    private final AiProviderChain aiChain;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiOrderService(ProductRepository productRepository, AiProviderChain aiChain) {
        this.productRepository = productRepository;
        this.aiChain = aiChain;
    }

    /** A product name + quantity extracted from free text (no catalog matching yet). */
    public record ExtractedItem(String name, BigDecimal qty) {
    }

    /**
     * Pure extraction engine: turn a free-text/Hinglish message into (name, qty) pairs.
     * Uses the LLM chain when available, else a regex fallback. Does NOT touch the catalog.
     */
    public List<ExtractedItem> extractItems(String text) {
        if (aiChain.isAvailable()) {
            try {
                List<ExtractedItem> llm = extractWithLlm(text);
                if (llm != null && !llm.isEmpty()) {
                    return llm;
                }
            } catch (Exception e) {
                log.warn("[AI] extraction failed, using regex: {}", e.getMessage());
            }
        }
        return extractWithRegex(text);
    }

    private List<ExtractedItem> extractWithLlm(String text) throws Exception {
        String system = "You are an order parser for an Indian grocery distributor. Output only JSON.";
        String user = """
                Extract the ordered items from the customer message below.
                The message may be in Hindi, English or Hinglish and may contain greetings or filler words.
                Return ONLY a JSON array; each element has:
                  "name": product name only (no quantity, no units like carton/peti/packet/kg),
                  "qty": numeric quantity, or 0 if the customer did not state a quantity.
                If there are no items, return [].
                Message: "%s"
                """.formatted(text.replace("\"", "'"));
        String json = extractJsonArray(aiChain.generate(system, user, 0.0));
        if (json == null) return List.of();
        JsonNode arr = mapper.readTree(json);
        if (!arr.isArray()) return List.of();
        List<ExtractedItem> out = new ArrayList<>();
        for (JsonNode node : arr) {
            String name = node.path("name").asText("").trim();
            if (!name.isEmpty()) {
                out.add(new ExtractedItem(name, parseQty(node.path("qty"))));
            }
        }
        return out;
    }

    private List<ExtractedItem> extractWithRegex(String text) {
        List<ExtractedItem> out = new ArrayList<>();
        for (String rawLine : text.split("[\\n,]+")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            BigDecimal qty = BigDecimal.ZERO; // 0 = quantity not stated
            Matcher m = QTY.matcher(line);
            if (m.find()) qty = new BigDecimal(m.group(1));
            String name = line.replaceAll("(\\d+(?:\\.\\d+)?)", "")
                    .replaceAll("(?i)\\b(pcs|kg|ltr|litre|carton|cartons|peti|packet|packets|box|boxes|bag|bags|x)\\b", "")
                    .trim();
            if (!name.isEmpty()) out.add(new ExtractedItem(name, qty));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ParsedOrderResponse parseText(TextOrderRequest req) {
        ParsedOrderResponse rules = parseWithRules(req);

        // 2) If it matched everything confidently, we're done — no LLM call (saves quota).
        boolean confident = !rules.matched().isEmpty()
                && rules.unmatched().isEmpty()
                && rules.matched().stream().allMatch(l -> l.confidence() >= CONFIDENCE_FLOOR);
        if (confident) {
            return rules;
        }

        // 3) Otherwise escalate to the LLM chain for messy / Hinglish / ambiguous text.
        if (aiChain.isAvailable()) {
            try {
                ParsedOrderResponse llm = parseWithLlm(req);
                if (llm != null) {
                    return llm;
                }
            } catch (Exception e) {
                log.warn("[AI] LLM parse failed, using rule-based result: {}", e.getMessage());
            }
        }
        return rules;
    }

    // ---------------- LLM-assisted parsing ----------------

    private ParsedOrderResponse parseWithLlm(TextOrderRequest req) throws Exception {
        String system = "You are an order parser for an Indian grocery distributor. Output only JSON.";
        String user = """
                Extract the ordered items from the customer message below.
                The message may be in Hindi, English or Hinglish and may contain greetings or filler words.
                Return ONLY a JSON array; each element has:
                  "name": product name only (no quantity, no units like carton/peti/packet/kg),
                  "qty": numeric quantity (default 1 if not stated).
                If there are no items, return [].
                Message: "%s"
                """.formatted(req.text().replace("\"", "'"));

        String raw = aiChain.generate(system, user, 0.0);
        String json = extractJsonArray(raw);
        if (json == null) return null;

        JsonNode arr = mapper.readTree(json);
        if (!arr.isArray()) return null;

        List<ParsedOrderResponse.ParsedLine> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (JsonNode node : arr) {
            String name = node.path("name").asText("").trim();
            if (name.isEmpty()) continue;
            BigDecimal qty = parseQty(node.path("qty"));
            matchInto(name, qty, req.supplierId(), matched, unmatched);
        }
        return new ParsedOrderResponse(matched, unmatched);
    }

    private BigDecimal parseQty(JsonNode qtyNode) {
        try {
            if (qtyNode.isNumber()) return BigDecimal.valueOf(qtyNode.asDouble());
            String s = qtyNode.asText("").replaceAll("[^0-9.]", "");
            return s.isEmpty() ? BigDecimal.ONE : new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    /** Pull the first JSON array out of a possibly fenced/explained LLM response. */
    private String extractJsonArray(String text) {
        if (text == null) return null;
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) return null;
        return text.substring(start, end + 1);
    }

    // ---------------- rule-based fallback ----------------

    private ParsedOrderResponse parseWithRules(TextOrderRequest req) {
        List<ParsedOrderResponse.ParsedLine> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();

        for (String rawLine : req.text().split("[\\n,]+")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            BigDecimal qty = BigDecimal.ONE;
            Matcher m = QTY.matcher(line);
            if (m.find()) {
                qty = new BigDecimal(m.group(1));
            }
            String name = line.replaceAll("(\\d+(?:\\.\\d+)?)", "")
                    .replaceAll("(?i)\\b(pcs|kg|ltr|litre|carton|cartons|packet|packets|box|boxes|x)\\b", "")
                    .trim();
            if (name.isEmpty()) {
                unmatched.add(line);
                continue;
            }
            matchInto(name, qty, req.supplierId(), matched, unmatched);
        }
        return new ParsedOrderResponse(matched, unmatched);
    }

    // ---------------- shared catalog matching ----------------

    private void matchInto(String name, BigDecimal qty, Long supplierId,
                           List<ParsedOrderResponse.ParsedLine> matched, List<String> unmatched) {
        List<Product> results = productRepository
                .search(name, null, supplierId, PageRequest.of(0, 1))
                .getContent();
        if (results.isEmpty()) {
            unmatched.add(name);
        } else {
            Product p = results.get(0);
            matched.add(new ParsedOrderResponse.ParsedLine(p.getId(), p.getName(), qty, confidence(name, p.getName())));
        }
    }

    private double confidence(String query, String productName) {
        String q = query.toLowerCase();
        String p = productName.toLowerCase();
        if (p.equals(q)) return 1.0;
        if (p.contains(q) || q.contains(p)) return 0.85;
        return 0.6;
    }
}
