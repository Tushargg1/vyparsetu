package com.vyaparsetu.catalog.controller;

import com.vyaparsetu.catalog.dto.ProductRequest;
import com.vyaparsetu.catalog.dto.ProductResponse;
import com.vyaparsetu.catalog.service.ProductService;
import com.vyaparsetu.common.response.ApiResponse;
import com.vyaparsetu.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Search products")
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.search(q, categoryId, supplierId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Lookup a product by barcode")
    public ApiResponse<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ApiResponse.ok(productService.getByBarcode(barcode));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok(null);
    }
}
