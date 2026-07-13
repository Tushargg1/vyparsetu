package com.vyaparsetu.sales.service;

import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.BusinessException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.inventory.entity.InventoryItem;
import com.vyaparsetu.inventory.repository.InventoryItemRepository;
import com.vyaparsetu.inventory.service.InventoryService;
import com.vyaparsetu.sales.dto.SalesDtos;
import com.vyaparsetu.sales.entity.CustomerSale;
import com.vyaparsetu.sales.entity.CustomerSaleItem;
import com.vyaparsetu.sales.entity.RetailPrice;
import com.vyaparsetu.sales.repository.CustomerSaleItemRepository;
import com.vyaparsetu.sales.repository.CustomerSaleRepository;
import com.vyaparsetu.sales.repository.RetailPriceRepository;
import com.vyaparsetu.sales.repository.RetailerDiscountRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesService {

    private final RetailPriceRepository retailPriceRepository;
    private final RetailerDiscountRepository discountRepository;
    private final CustomerSaleRepository saleRepository;
    private final CustomerSaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final UserService userService;

    public SalesService(RetailPriceRepository retailPriceRepository, RetailerDiscountRepository discountRepository,
                        CustomerSaleRepository saleRepository,
                        CustomerSaleItemRepository saleItemRepository, ProductRepository productRepository,
                        InventoryItemRepository inventoryItemRepository, InventoryService inventoryService,
                        UserService userService) {
        this.retailPriceRepository = retailPriceRepository;
        this.discountRepository = discountRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
        this.userService = userService;
    }

    // ---------------- Reusable discounts ----------------

    @Transactional(readOnly = true)
    public List<SalesDtos.DiscountResponse> discounts() {
        Long retailerId = userService.currentRetailerId();
        return discountRepository.findByRetailerId(retailerId).stream()
                .map(d -> new SalesDtos.DiscountResponse(d.getId(), d.getLabel(), d.getPercent()))
                .toList();
    }

    @Transactional
    public SalesDtos.DiscountResponse addDiscount(SalesDtos.DiscountRequest req) {
        Long retailerId = userService.currentRetailerId();
        com.vyaparsetu.sales.entity.RetailerDiscount d = new com.vyaparsetu.sales.entity.RetailerDiscount();
        d.setRetailerId(retailerId);
        d.setLabel(req.label());
        d.setPercent(req.percent());
        d = discountRepository.save(d);
        return new SalesDtos.DiscountResponse(d.getId(), d.getLabel(), d.getPercent());
    }

    @Transactional
    public void deleteDiscount(Long id) {
        Long retailerId = userService.currentRetailerId();
        discountRepository.findById(id)
                .filter(d -> d.getRetailerId().equals(retailerId))
                .ifPresent(discountRepository::delete);
    }

    private BigDecimal priceFor(Long retailerId, Product p) {
        return retailPriceRepository.findByRetailerIdAndProductId(retailerId, p.getId())
                .map(RetailPrice::getPrice)
                .orElse(p.getMrp());
    }

    @Transactional(readOnly = true)
    public SalesDtos.LookupResponse lookup(String barcode) {
        Long retailerId = userService.currentRetailerId();
        Product p = productRepository.findByBarcodeAndActiveTrue(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product with barcode", barcode));
        BigDecimal stock = inventoryItemRepository.findByRetailerIdAndProductId(retailerId, p.getId())
                .map(InventoryItem::getQuantity).orElse(BigDecimal.ZERO);
        return new SalesDtos.LookupResponse(p.getId(), p.getName(), p.getBrand(), priceFor(retailerId, p), stock);
    }

    @Transactional
    public SalesDtos.SaleResponse recordSale(SalesDtos.RecordSaleRequest req) {
        Long retailerId = userService.currentRetailerId();
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("EMPTY_SALE", HttpStatus.BAD_REQUEST, "No items to sell");
        }
        CustomerSale sale = new CustomerSale();
        sale.setRetailerId(retailerId);
        sale = saleRepository.save(sale);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalItems = BigDecimal.ZERO;
        List<SalesDtos.SaleItemResponse> lines = new ArrayList<>();
        for (SalesDtos.SaleLine line : req.items()) {
            Product p = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", line.productId()));
            // Decrement stock (throws if insufficient); capture cost for profit.
            InventoryItem item = inventoryService.applyMovement(retailerId, p.getId(), Enums.MovementType.SALE,
                    line.quantity(), null, null, null, "Counter sale #" + sale.getId(), "SALE", sale.getId());

            BigDecimal unitPrice = priceFor(retailerId, p);
            BigDecimal lineTotal = unitPrice.multiply(line.quantity()).setScale(2, RoundingMode.HALF_UP);

            CustomerSaleItem si = new CustomerSaleItem();
            si.setSaleId(sale.getId());
            si.setProductId(p.getId());
            si.setProductName(p.getName());
            si.setQuantity(line.quantity());
            si.setUnitPrice(unitPrice);
            si.setCostPrice(item.getCostPrice());
            si.setLineTotal(lineTotal);
            saleItemRepository.save(si);

            total = total.add(lineTotal);
            totalItems = totalItems.add(line.quantity());
            lines.add(new SalesDtos.SaleItemResponse(p.getId(), p.getName(), line.quantity(), unitPrice, item.getCostPrice(), lineTotal));
        }
        sale.setTotalAmount(total);
        sale.setTotalItems(totalItems);
        sale = saleRepository.save(sale);

        return new SalesDtos.SaleResponse(sale.getId(), total, totalItems, sale.getCreatedAt(), lines);
    }

    @Transactional(readOnly = true)
    public List<SalesDtos.SaleResponse> history(Pageable pageable) {
        Long retailerId = userService.currentRetailerId();
        return saleRepository.findByRetailerIdOrderByCreatedAtDesc(retailerId, pageable).stream()
                .map(s -> {
                    List<SalesDtos.SaleItemResponse> items = saleItemRepository.findBySaleId(s.getId()).stream()
                            .map(it -> new SalesDtos.SaleItemResponse(it.getProductId(), it.getProductName(),
                                    it.getQuantity(), it.getUnitPrice(), it.getCostPrice(), it.getLineTotal()))
                            .toList();
                    return new SalesDtos.SaleResponse(s.getId(), s.getTotalAmount(), s.getTotalItems(),
                            s.getCreatedAt(), items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesDtos.RateListItem> rateList() {
        Long retailerId = userService.currentRetailerId();
        List<InventoryItem> items = inventoryItemRepository.findByRetailerId(retailerId);
        List<Product> products = productRepository.findAllById(
                items.stream().map(InventoryItem::getProductId).toList());
        java.util.Map<Long, Product> pmap = new java.util.HashMap<>();
        products.forEach(p -> pmap.put(p.getId(), p));
        List<SalesDtos.RateListItem> out = new ArrayList<>();
        for (InventoryItem it : items) {
            Product p = pmap.get(it.getProductId());
            if (p == null) continue;
            BigDecimal myPrice = retailPriceRepository.findByRetailerIdAndProductId(retailerId, p.getId())
                    .map(RetailPrice::getPrice).orElse(null);
            out.add(new SalesDtos.RateListItem(p.getId(), p.getName(), p.getBrand(),
                    p.getMrp(), p.getSellingPrice(), myPrice, it.getQuantity()));
        }
        return out;
    }

    @Transactional
    public void setRate(SalesDtos.SetRateRequest req) {
        Long retailerId = userService.currentRetailerId();
        productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));
        RetailPrice rp = retailPriceRepository.findByRetailerIdAndProductId(retailerId, req.productId())
                .orElseGet(() -> {
                    RetailPrice n = new RetailPrice();
                    n.setRetailerId(retailerId);
                    n.setProductId(req.productId());
                    return n;
                });
        rp.setPrice(req.price());
        retailPriceRepository.save(rp);
    }
}
