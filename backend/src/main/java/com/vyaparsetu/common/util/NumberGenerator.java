package com.vyaparsetu.common.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class NumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private NumberGenerator() {
    }

    public static String orderNumber() {
        return "ORD-" + LocalDate.now().format(DATE) + "-" + randomDigits(5);
    }

    public static String invoiceNumber() {
        return "INV-" + LocalDate.now().format(DATE) + "-" + randomDigits(5);
    }

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /** Human-friendly, unambiguous distributor invite code, e.g. "VS-7QF3K9P2". */
    public static String inviteCode() {
        StringBuilder sb = new StringBuilder("VS-");
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
