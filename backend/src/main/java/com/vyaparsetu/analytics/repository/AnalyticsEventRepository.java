package com.vyaparsetu.analytics.repository;

import com.vyaparsetu.analytics.entity.AnalyticsEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countBySupplierIdAndEventType(Long supplierId, AnalyticsEvent.EventType eventType);

    /** Most frequently ordered products: [productId, orderCount]. */
    @Query("""
            select e.productId, count(e)
            from AnalyticsEvent e
            where e.supplierId = :supplierId and e.eventType = com.vyaparsetu.analytics.entity.AnalyticsEvent$EventType.PRODUCT_ORDERED
              and e.productId is not null
            group by e.productId
            order by count(e) desc
            """)
    List<Object[]> topProducts(@Param("supplierId") Long supplierId, Pageable pageable);

    /** Average order value across ORDER_PLACED events. */
    @Query("""
            select avg(e.numericValue)
            from AnalyticsEvent e
            where e.supplierId = :supplierId and e.eventType = com.vyaparsetu.analytics.entity.AnalyticsEvent$EventType.ORDER_PLACED
            """)
    Double averageOrderValue(@Param("supplierId") Long supplierId);

    /** Order count per retailer (repeat-customer signal): [retailerId, orderCount]. */
    @Query("""
            select e.retailerId, count(e)
            from AnalyticsEvent e
            where e.supplierId = :supplierId and e.eventType = com.vyaparsetu.analytics.entity.AnalyticsEvent$EventType.ORDER_PLACED
              and e.retailerId is not null
            group by e.retailerId
            order by count(e) desc
            """)
    List<Object[]> ordersPerRetailer(@Param("supplierId") Long supplierId, Pageable pageable);

    /** Most common alias / validation-failure text values: [textValue, count]. */
    @Query("""
            select e.textValue, count(e)
            from AnalyticsEvent e
            where e.supplierId = :supplierId and e.eventType = :type and e.textValue is not null
            group by e.textValue
            order by count(e) desc
            """)
    List<Object[]> topTextValues(@Param("supplierId") Long supplierId,
                                 @Param("type") AnalyticsEvent.EventType type, Pageable pageable);
}
