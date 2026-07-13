package com.vyaparsetu.payment.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.payment.dto.PaymentConfirmRequest;
import com.vyaparsetu.payment.dto.PaymentInitRequest;
import com.vyaparsetu.payment.dto.PaymentResponse;
import com.vyaparsetu.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Order payments, wallet and credit")
@PreAuthorize("hasRole('RETAILER')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/init")
    @Operation(summary = "Initiate a payment for an order")
    public ApiResponse<PaymentResponse> init(@Valid @RequestBody PaymentInitRequest req) {
        return ApiResponse.ok(paymentService.init(req));
    }

    @PostMapping("/{uuid}/confirm")
    @Operation(summary = "Confirm a payment (idempotent)")
    public ApiResponse<PaymentResponse> confirm(@PathVariable String uuid,
                                               @Valid @RequestBody PaymentConfirmRequest req) {
        return ApiResponse.ok(paymentService.confirm(uuid, req.gatewayRef()));
    }
}
