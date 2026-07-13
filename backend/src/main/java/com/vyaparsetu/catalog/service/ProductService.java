package com.vyaparsetu.catalog.service;

import com.vyaparsetu.catalog.dto.ProductRequest;
import com.vyaparsetu.catalog.dto.ProductResponse;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.common.response.PageResponse;
import com.vyaparsetu.common.security.CurrentUser;
import com.vyaparsetu.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;

    public ProductService(ProductRepository productRepository, UserService userService) {
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String q, Long categoryId, Long supplierId, int page, int size) {
        // ISOLATION: retailers only see their distributor's catalog; suppliers only their own.
        Long scopedSupplierId = resolveSupplierScope(supplierId);
        Page<Product> result = productRepository.search(
                (q == null || q.isBlank()) ? null : q,
                categoryId, scopedSupplierId,
                PageRequest.of(page, size, Sort.by("name").ascending()));
        return PageResponse.from(result.map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product p = find(id);
        ensureVisible(p);
        return ProductResponse.from(p);
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Long scope = resolveSupplierScope(null);
        Product p = (scope == null)
                ? productRepository.findByBarcodeAndActiveTrue(barcode).orElse(null)
                : productRepository.findByBarcodeAndSupplierIdAndActiveTrue(barcode, scope).orElse(null);
        if (p == null) {
            throw new ResourceNotFoundException("Product with barcode", barcode);
        }
        return ProductResponse.from(p);
    }

    /**
     * Resolves which supplier's catalog the caller may see.
     * RETAILER -> the requested supplier (or all distributors when none given);
     * SUPPLIER -> themselves; ADMIN -> the requested id (or all).
     */
    private Long resolveSupplierScope(Long requestedSupplierId) {
        var roles = CurrentUser.get().roles();
        if (roles.contains("SUPPLIER") && !roles.contains("ADMIN")) {
            return userService.currentSupplierId();
        }
        // RETAILER and ADMIN may browse across all distributors, or filter by one.
        return requestedSupplierId;
    }

    private void ensureVisible(Product p) {
        var roles = CurrentUser.get().roles();
        // A supplier may only view their own products; retailers/admin see any active product.
        if (roles.contains("SUPPLIER") && !roles.contains("ADMIN")
                && !userService.currentSupplierId().equals(p.getSupplierId())) {
            throw new ResourceNotFoundException("Product", p.getId());
        }
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        Long supplierId = userService.currentSupplierId();
        Product p = new Product();
        p.setSupplierId(supplierId);
        apply(p, req);
        return ProductResponse.from(productRepository.save(p));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = find(id);
        ensureOwner(p);
        apply(p, req);
        return ProductResponse.from(productRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Product p = find(id);
        ensureOwner(p);
        p.setActive(false);
        productRepository.save(p);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setBrand(req.brand());
        p.setCategoryId(req.categoryId());
        p.setBarcode(req.barcode());
        p.setSku(req.sku());
        if (req.unit() != null) p.setUnit(req.unit());
        p.setPackSize(req.packSize());
        p.setMrp(req.mrp());
        p.setSellingPrice(req.sellingPrice());
        p.setGstRate(req.gstRate() != null ? req.gstRate() : BigDecimal.ZERO);
        p.setHsnCode(req.hsnCode());
        p.setImageUrl(req.imageUrl());
        if (req.stockQty() != null) p.setStockQty(req.stockQty());
        if (req.lowStockThreshold() != null) p.setLowStockThreshold(req.lowStockThreshold());
        if (req.trackStock() != null) p.setTrackStock(req.trackStock());
    }

    private void ensureOwner(Product p) {
        if (!p.getSupplierId().equals(userService.currentSupplierId())) {
            throw new AccessDeniedException("Not your product");
        }
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
