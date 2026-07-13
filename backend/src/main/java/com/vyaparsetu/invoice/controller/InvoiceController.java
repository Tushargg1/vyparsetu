package com.vyaparsetu.invoice.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.invoice.entity.Invoice;
import com.vyaparsetu.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "GST invoice generation")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @Operation(summary = "Generate (or fetch) the invoice for an order")
    public ApiResponse<Invoice> generate(@PathVariable Long orderId) {
        return ApiResponse.ok(invoiceService.generateForOrder(orderId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get the invoice for an order")
    public ApiResponse<Invoice> getByOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(invoiceService.getByOrder(orderId));
    }

    @GetMapping(value = "/order/{orderId}/document", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Download the rendered invoice (ownership-checked)")
    public ResponseEntity<String> document(@PathVariable Long orderId) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(invoiceService.getDocument(orderId));
    }
}
