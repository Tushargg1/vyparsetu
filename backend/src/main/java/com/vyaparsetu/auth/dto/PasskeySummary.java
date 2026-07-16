package com.vyaparsetu.auth.dto;

import java.time.Instant;

public record PasskeySummary(Long id, String name, Instant createdAt, Instant lastUsedAt) {
}
