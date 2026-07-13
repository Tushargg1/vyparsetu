package com.vyaparsetu.whatsapp.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Serialized conversation context stored in {@code whatsapp_sessions.data_json}. */
@Data
public class WhatsAppSessionData {

    /** Numbered options currently presented to the user (1-based on display). */
    private List<Opt> options = new ArrayList<>();

    /** The draft order being built (shared by AI and menu flows). */
    private List<Line> draft = new ArrayList<>();

    /** Queue of ambiguous extracted terms awaiting variant selection. */
    private List<Amb> ambiguous = new ArrayList<>();

    /** Product chosen in the menu flow, awaiting a quantity. */
    private Long pendingProductId;
    private String pendingProductName;

    /** Registration sub-flow state (for unknown numbers). */
    private String regType;       // NEW | LINK
    private String regShopName;
    private String regOwner;

    /** A draft line awaiting a quantity (validation: missing quantity). */
    private String pendingQtyTerm;

    /** The resolved retailer for the current conversation (for analytics attribution). */
    private Long retailerId;

    @Data
    public static class Opt {
        private String label;
        private Long ref;     // productId / categoryId (nullable for action codes)
        private String code;  // action code for menu/review options
        public Opt() {}
        public Opt(String label, Long ref, String code) {
            this.label = label; this.ref = ref; this.code = code;
        }
    }

    @Data
    public static class Line {
        private Long productId;
        private String name;
        private double qty;
        private double price;
        private double gstRate;
        public Line() {}
        public Line(Long productId, String name, double qty, double price, double gstRate) {
            this.productId = productId; this.name = name; this.qty = qty;
            this.price = price; this.gstRate = gstRate;
        }
    }

    @Data
    public static class Amb {
        private String term;
        private double qty;
        private List<Long> candidates = new ArrayList<>();
        public Amb() {}
        public Amb(String term, double qty, List<Long> candidates) {
            this.term = term; this.qty = qty; this.candidates = candidates;
        }
    }
}
