package com.vyaparsetu.auth.dto;

import java.util.List;

public record SecurityMethodsResponse(boolean totpEnabled, List<PasskeySummary> passkeys) {
}
