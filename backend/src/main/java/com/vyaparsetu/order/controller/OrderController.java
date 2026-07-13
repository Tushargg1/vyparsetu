package com.vyaparsetu.order.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.response.PageResponse;
import com.vyaparsetu.order.dto.OrderResponse;
import com.vyaparsetu.order.dto.OrderStatusUpdateRequest;
import com.vyaparsetu.order.dto.PlaceOrderRequest;
import com.vyaparsetu.order.entity.OrderStatus;
import com.vyaparsetu.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order placement and lifecycle")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "Place an order from the cart")
    public ApiResponse<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest req) {
        return ApiResponse.ok(orderService.placeOrder(req));
    }

    @GetMapping
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "List my orders")
    public ApiResponse<PageResponse<OrderResponse>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.myOrders(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getById(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get an order's status-change timeline")
    public ApiResponse<java.util.List<OrderService.HistoryEntry>> history(@PathVariable Long id) {
        return ApiResponse.ok(orderService.history(id));
    }

    @GetMapping("/supplier")
    @PreAuthorize("hasRole('SUPPLIER')")
    @Operation(summary = "List orders received by the current supplier")
    public ApiResponse<PageResponse<OrderResponse>> supplierOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.supplierOrders(status, page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition an order to a new status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id,
                                                   @Valid @RequestBody OrderStatusUpdateRequest req) {
        return ApiResponse.ok(orderService.updateStatus(id, req.status(), req.note()));
    }

    @PostMapping("/repeat")
    @PreAuthorize("hasRole('RETAILER')")
    @Operation(summary = "Repeat the last order into the cart")
    public ApiResponse<OrderResponse> repeat() {
        return ApiResponse.ok(orderService.repeatLast());
    }

    public record ModifyOrderRequest(java.util.List<Item> items) {
        public record Item(Long productId, java.math.BigDecimal quantity) {
        }
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "Modify the items of an order (allowed before packing)")
    public ApiResponse<OrderResponse> modify(@PathVariable Long id,
                                             @RequestBody ModifyOrderRequest req) {
        var lines = req.items().stream()
                .map(i -> new OrderService.LineItem(i.productId(), i.quantity()))
                .toList();
        return ApiResponse.ok(orderService.modifyOrder(id, lines));
    }
}
