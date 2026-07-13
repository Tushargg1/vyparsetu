package com.vyaparsetu.ai.controller;

import com.vyaparsetu.ai.dto.ParsedOrderResponse;
import com.vyaparsetu.ai.dto.TextOrderRequest;
import com.vyaparsetu.ai.service.AiClient;
import com.vyaparsetu.ai.service.AiOrderService;
import com.vyaparsetu.ai.service.ForecastService;
import com.vyaparsetu.ai.service.RecommendationService;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "AI ordering, recommendations and chat assistant")
@PreAuthorize("hasRole('RETAILER')")
public class AiController {

    private final AiOrderService aiOrderService;
    private final RecommendationService recommendationService;
    private final ForecastService forecastService;
    private final AiClient aiClient;

    public AiController(AiOrderService aiOrderService, RecommendationService recommendationService,
                        ForecastService forecastService, AiClient aiClient) {
        this.aiOrderService = aiOrderService;
        this.recommendationService = recommendationService;
        this.forecastService = forecastService;
        this.aiClient = aiClient;
    }

    @PostMapping("/order/text")
    @Operation(summary = "Convert a free-text shopping list into order lines")
    public ApiResponse<ParsedOrderResponse> textToOrder(@Valid @RequestBody TextOrderRequest req) {
        return ApiResponse.ok(aiOrderService.parseText(req));
    }

    @PostMapping("/order/voice")
    @Operation(summary = "Convert a voice transcript (client-side STT) into order lines")
    public ApiResponse<ParsedOrderResponse> voiceToOrder(@Valid @RequestBody TextOrderRequest req) {
        return ApiResponse.ok(aiOrderService.parseText(req));
    }

    @PostMapping("/order/image")
    @Operation(summary = "Convert extracted text from a handwritten list (client-side OCR) into order lines")
    public ApiResponse<ParsedOrderResponse> imageToOrder(@Valid @RequestBody TextOrderRequest req) {
        return ApiResponse.ok(aiOrderService.parseText(req));
    }

    @GetMapping("/recommendations/reorder")
    @Operation(summary = "Smart reorder suggestions based on inventory")
    public ApiResponse<List<RecommendationService.Recommendation>> reorder() {
        return ApiResponse.ok(recommendationService.smartReorder());
    }

    @GetMapping("/forecast")
    @Operation(summary = "Demand forecast: days-to-stockout and next purchase date")
    public ApiResponse<List<ForecastService.Forecast>> forecast() {
        return ApiResponse.ok(forecastService.forecast());
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with the AI assistant")
    public ApiResponse<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        return ApiResponse.ok(Map.of("reply", aiClient.complete(message)));
    }
}
