package com.vyaparsetu.order.controller;

import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.order.dto.CartItemRequest;
import com.vyaparsetu.order.dto.CartResponse;
import com.vyaparsetu.order.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Retailer cart")
@PreAuthorize("hasRole('RETAILER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(@RequestParam Long supplierId) {
        return ApiResponse.ok(cartService.getCart(supplierId));
    }

    @GetMapping("/all")
    public ApiResponse<java.util.List<CartResponse>> myCarts() {
        return ApiResponse.ok(cartService.myCarts());
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartItemRequest req) {
        return ApiResponse.ok(cartService.addItem(req));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long cartItemId) {
        cartService.removeItem(cartItemId);
        return ApiResponse.ok(null);
    }
}
