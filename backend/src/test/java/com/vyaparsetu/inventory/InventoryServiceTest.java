package com.vyaparsetu.inventory;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.common.exception.InsufficientStockException;
import com.vyaparsetu.catalog.repository.ProductRepository;
import com.vyaparsetu.inventory.entity.InventoryItem;
import com.vyaparsetu.inventory.repository.InventoryBatchRepository;
import com.vyaparsetu.inventory.repository.InventoryItemRepository;
import com.vyaparsetu.inventory.repository.StockMovementRepository;
import com.vyaparsetu.inventory.service.InventoryService;
import com.vyaparsetu.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Property 1: stock can never become negative. */
class InventoryServiceTest {

    private InventoryItemRepository itemRepository;
    private InventoryService service;

    @BeforeEach
    void setup() {
        itemRepository = mock(InventoryItemRepository.class);
        InventoryBatchRepository batchRepository = mock(InventoryBatchRepository.class);
        StockMovementRepository movementRepository = mock(StockMovementRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        UserService userService = mock(UserService.class);
        service = new InventoryService(itemRepository, batchRepository, movementRepository, productRepository, userService);
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void purchaseIncreasesStock() {
        when(itemRepository.findByRetailerIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        InventoryItem result = service.applyMovement(1L, 10L, Enums.MovementType.PURCHASE,
                new BigDecimal("5"), null, null, null, null, null, null);
        assertEquals(0, new BigDecimal("5").compareTo(result.getQuantity()));
    }

    @Test
    void saleBeyondStockIsRejected() {
        InventoryItem existing = new InventoryItem();
        existing.setRetailerId(1L);
        existing.setProductId(10L);
        existing.setQuantity(new BigDecimal("3"));
        when(itemRepository.findByRetailerIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        assertThrows(InsufficientStockException.class, () ->
                service.applyMovement(1L, 10L, Enums.MovementType.SALE,
                        new BigDecimal("5"), null, null, null, null, null, null));
    }

    @Test
    void saleWithinStockSucceeds() {
        InventoryItem existing = new InventoryItem();
        existing.setRetailerId(1L);
        existing.setProductId(10L);
        existing.setQuantity(new BigDecimal("10"));
        when(itemRepository.findByRetailerIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        InventoryItem result = service.applyMovement(1L, 10L, Enums.MovementType.SALE,
                new BigDecimal("4"), null, null, null, null, null, null);
        assertEquals(0, new BigDecimal("6").compareTo(result.getQuantity()));
    }
}
