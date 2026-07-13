package com.vyaparsetu.inventory.service;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.InsufficientStockException;
import com.vyaparsetu.common.exception.ResourceNotFoundException;
import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.inventory.dto.InventoryItemResponse;
import com.vyaparsetu.inventory.dto.StockMovementRequest;
import com.vyaparsetu.inventory.entity.InventoryBatch;
import com.vyaparsetu.inventory.entity.InventoryItem;
import com.vyaparsetu.inventory.entity.StockMovement;
import com.vyaparsetu.inventory.repository.InventoryBatchRepository;
import com.vyaparsetu.inventory.repository.InventoryItemRepository;
import com.vyaparsetu.inventory.repository.StockMovementRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/**
 * The single entry point for all stock mutations. No other module writes
 * inventory_items directly. Every change records a StockMovement and keeps
 * the item quantity non-negative (Property 1 and Property 2).
 */
@Service
public class InventoryService {

    private static final EnumSet<Enums.MovementType> INCREASES =
            EnumSet.of(Enums.MovementType.PURCHASE, Enums.MovementType.RETURN, Enums.MovementType.ADJUSTMENT);

    private final InventoryItemRepository itemRepository;
    private final InventoryBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public InventoryService(InventoryItemRepository itemRepository,
                            InventoryBatchRepository batchRepository,
                            StockMovementRepository movementRepository,
                            ProductRepository productRepository,
                            UserService userService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> myInventory() {
        return itemRepository.findByRetailerId(userService.currentRetailerId())
                .stream().map(InventoryItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> myLowStock() {
        return itemRepository.findLowStock(userService.currentRetailerId())
                .stream().map(InventoryItemResponse::from).toList();
    }

    public record ExpiringBatch(Long productId, String batchNumber, BigDecimal quantity, LocalDate expiryDate) {
    }

    /** Batches expiring within the given number of days (for the current retailer's items). */
    @Transactional(readOnly = true)
    public List<ExpiringBatch> expiringSoon(int days) {
        Long retailerId = userService.currentRetailerId();
        List<Long> itemIds = itemRepository.findByRetailerId(retailerId).stream()
                .map(InventoryItem::getId).toList();
        if (itemIds.isEmpty()) return List.of();
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return batchRepository.findByExpiryDateBeforeAndQuantityGreaterThan(cutoff, BigDecimal.ZERO).stream()
                .filter(b -> itemIds.contains(b.getInventoryItemId()))
                .map(b -> new ExpiringBatch(
                        itemRepository.findById(b.getInventoryItemId()).map(InventoryItem::getProductId).orElse(null),
                        b.getBatchNumber(), b.getQuantity(), b.getExpiryDate()))
                .toList();
    }

    /** Scan a product barcode to record a sale; auto-decrements inventory. */
    @Transactional
    public InventoryItemResponse sellByBarcode(String barcode, BigDecimal quantity) {
        Long retailerId = userService.currentRetailerId();
        Product p = productRepository.findByBarcodeAndActiveTrue(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product with barcode", barcode));
        InventoryItem item = applyMovement(retailerId, p.getId(), Enums.MovementType.SALE,
                quantity == null ? BigDecimal.ONE : quantity, null, null, null,
                "Sold to customer (scan)", "SALE", null);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse applyMovement(StockMovementRequest req) {
        Long retailerId = userService.currentRetailerId();
        InventoryItem item = applyMovement(retailerId, req.productId(), req.movementType(),
                req.quantity(), req.costPrice(), req.batchNumber(), req.expiryDate(),
                req.note(), null, null);
        return InventoryItemResponse.from(item);
    }

    /**
     * Core mutation usable by other modules (e.g. order fulfilment).
     */
    @Transactional
    public InventoryItem applyMovement(Long retailerId, Long productId, Enums.MovementType type,
                                       BigDecimal quantity, BigDecimal costPrice,
                                       String batchNumber, java.time.LocalDate expiryDate,
                                       String note, String referenceType, Long referenceId) {
        BigDecimal magnitude = quantity.abs();
        boolean increase = INCREASES.contains(type);
        BigDecimal delta = increase ? magnitude : magnitude.negate();

        InventoryItem item = itemRepository.findByRetailerIdAndProductId(retailerId, productId)
                .orElseGet(() -> {
                    InventoryItem ni = new InventoryItem();
                    ni.setRetailerId(retailerId);
                    ni.setProductId(productId);
                    ni.setQuantity(BigDecimal.ZERO);
                    return ni;
                });

        BigDecimal newQty = item.getQuantity().add(delta);
        if (newQty.signum() < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + ": have "
                            + item.getQuantity() + ", requested " + magnitude);
        }
        item.setQuantity(newQty);
        if (costPrice != null) {
            item.setCostPrice(costPrice);
        }
        item = itemRepository.save(item);

        Long batchId = handleBatch(item, type, magnitude, increase, batchNumber, expiryDate, costPrice);

        StockMovement movement = new StockMovement();
        movement.setInventoryItemId(item.getId());
        movement.setBatchId(batchId);
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movementRepository.save(movement);

        return item;
    }

    private Long handleBatch(InventoryItem item, Enums.MovementType type, BigDecimal magnitude,
                             boolean increase, String batchNumber, java.time.LocalDate expiryDate,
                             BigDecimal costPrice) {
        if (increase && (batchNumber != null || expiryDate != null)) {
            InventoryBatch batch = new InventoryBatch();
            batch.setInventoryItemId(item.getId());
            batch.setBatchNumber(batchNumber);
            batch.setExpiryDate(expiryDate);
            batch.setQuantity(magnitude);
            batch.setCostPrice(costPrice);
            return batchRepository.save(batch).getId();
        }
        if (!increase) {
            depleteFefo(item.getId(), magnitude);
        }
        return null;
    }

    /** Deplete batches first-expiry-first-out. Best effort: only touches tracked batches. */
    private void depleteFefo(Long itemId, BigDecimal magnitude) {
        BigDecimal remaining = magnitude;
        List<InventoryBatch> batches = batchRepository
                .findByInventoryItemIdAndQuantityGreaterThanOrderByExpiryDateAsc(itemId, BigDecimal.ZERO);
        for (InventoryBatch b : batches) {
            if (remaining.signum() <= 0) break;
            BigDecimal take = b.getQuantity().min(remaining);
            b.setQuantity(b.getQuantity().subtract(take));
            batchRepository.save(b);
            remaining = remaining.subtract(take);
        }
    }
}
