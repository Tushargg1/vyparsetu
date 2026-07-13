package com.vyaparsetu.report.repository;

import com.vyaparsetu.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read-only aggregate queries for reporting. Backed by the Order entity but
 * also runs native aggregates over the stock-movement ledger.
 */
public interface ReportRepository extends JpaRepository<Order, Long> {

    // Retailer purchases (orders placed) in a period
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o " +
            "WHERE o.retailerId = :rid AND o.placedAt BETWEEN :from AND :to")
    BigDecimal retailerPurchaseValue(@Param("rid") Long retailerId,
                                     @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.retailerId = :rid AND o.placedAt BETWEEN :from AND :to")
    long retailerOrderCount(@Param("rid") Long retailerId,
                            @Param("from") Instant from, @Param("to") Instant to);

    // Supplier sales (orders received) in a period
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o " +
            "WHERE o.supplierId = :sid AND o.placedAt BETWEEN :from AND :to")
    BigDecimal supplierSalesValue(@Param("sid") Long supplierId,
                                  @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.supplierId = :sid AND o.placedAt BETWEEN :from AND :to")
    long supplierOrderCount(@Param("sid") Long supplierId,
                            @Param("from") Instant from, @Param("to") Instant to);

    // Retailer in-shop sales value from recorded counter sales (uses retailer's retail price)
    @Query(value = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM customer_sales
            WHERE retailer_id = :rid AND created_at BETWEEN :from AND :to
            """, nativeQuery = true)
    BigDecimal retailerSalesValue(@Param("rid") Long retailerId,
                                  @Param("from") Instant from, @Param("to") Instant to);

    // Cost of goods sold for those sales (valued at captured cost price)
    @Query(value = """
            SELECT COALESCE(SUM(si.quantity * COALESCE(si.cost_price, 0)), 0)
            FROM customer_sale_items si
            JOIN customer_sales s ON si.sale_id = s.id
            WHERE s.retailer_id = :rid AND s.created_at BETWEEN :from AND :to
            """, nativeQuery = true)
    BigDecimal retailerCogs(@Param("rid") Long retailerId,
                            @Param("from") Instant from, @Param("to") Instant to);

    // Current inventory value at cost
    @Query(value = """
            SELECT COALESCE(SUM(quantity * COALESCE(cost_price, 0)), 0)
            FROM inventory_items WHERE retailer_id = :rid
            """, nativeQuery = true)
    BigDecimal inventoryValue(@Param("rid") Long retailerId);

    interface ProductSold {
        Long getProductId();
        String getProductName();
        BigDecimal getSold();
    }

    // Retailer's best sellers (units sold to customers, from recorded sales)
    @Query(value = """
            SELECT si.product_id AS productId, si.product_name AS productName,
                   COALESCE(SUM(si.quantity), 0) AS sold
            FROM customer_sale_items si
            JOIN customer_sales s ON si.sale_id = s.id
            WHERE s.retailer_id = :rid
            GROUP BY si.product_id, si.product_name
            ORDER BY sold DESC
            LIMIT 10
            """, nativeQuery = true)
    List<ProductSold> retailerTopProducts(@Param("rid") Long retailerId);

    // Distributor's most-ordered products (units ordered by retailers)
    @Query(value = """
            SELECT oi.product_id AS productId, oi.product_name AS productName,
                   COALESCE(SUM(oi.quantity), 0) AS sold
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.supplier_id = :sid
            GROUP BY oi.product_id, oi.product_name
            ORDER BY sold DESC
            LIMIT 10
            """, nativeQuery = true)
    List<ProductSold> supplierTopProducts(@Param("sid") Long supplierId);
}
