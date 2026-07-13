package com.vyaparsetu.analytics.service;

import com.vyaparsetu.analytics.entity.AnalyticsEvent;
import com.vyaparsetu.analytics.repository.AnalyticsEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Records analytics events and exposes aggregate queries.
 * Recording is best-effort and never blocks the ordering flow.
 */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;

    public AnalyticsService(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Long supplierId, Long retailerId, AnalyticsEvent.EventType type,
                       Long productId, BigDecimal numericValue, String textValue) {
        if (supplierId == null) return;
        AnalyticsEvent e = new AnalyticsEvent();
        e.setSupplierId(supplierId);
        e.setRetailerId(retailerId);
        e.setEventType(type);
        e.setProductId(productId);
        e.setNumericValue(numericValue);
        e.setTextValue(textValue != null && textValue.length() > 512
                ? textValue.substring(0, 512) : textValue);
        repository.save(e);
    }

    /** Convenience helpers used by callers that only care about one dimension. */
    @Transactional
    public void recordOrderPlaced(Long supplierId, Long retailerId, BigDecimal orderValue) {
        record(supplierId, retailerId, AnalyticsEvent.EventType.ORDER_PLACED, null, orderValue, null);
    }

    @Transactional
    public void recordProductOrdered(Long supplierId, Long retailerId, Long productId, BigDecimal qty) {
        record(supplierId, retailerId, AnalyticsEvent.EventType.PRODUCT_ORDERED, productId, qty, null);
    }

    @Transactional
    public void recordAiExtractionFailed(Long supplierId, Long retailerId, String rawText) {
        record(supplierId, retailerId, AnalyticsEvent.EventType.AI_EXTRACTION_FAILED, null, null, rawText);
    }

    @Transactional
    public void recordValidationFailure(Long supplierId, Long retailerId, String detail) {
        record(supplierId, retailerId, AnalyticsEvent.EventType.VALIDATION_FAILURE, null, null, detail);
    }

    @Transactional
    public void recordAliasUsed(Long supplierId, Long retailerId, String alias) {
        record(supplierId, retailerId, AnalyticsEvent.EventType.ALIAS_USED, null, null, alias);
    }

    /** Aggregated summary for a distributor dashboard. */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long supplierId) {
        Double aov = repository.averageOrderValue(supplierId);
        long extractionFailures =
                repository.countBySupplierIdAndEventType(supplierId, AnalyticsEvent.EventType.AI_EXTRACTION_FAILED);
        long validationFailures =
                repository.countBySupplierIdAndEventType(supplierId, AnalyticsEvent.EventType.VALIDATION_FAILURE);

        List<Object[]> topProducts = repository.topProducts(supplierId, PageRequest.of(0, 10));
        List<Object[]> repeatCustomers = repository.ordersPerRetailer(supplierId, PageRequest.of(0, 10));
        List<Object[]> topAliases =
                repository.topTextValues(supplierId, AnalyticsEvent.EventType.ALIAS_USED, PageRequest.of(0, 10));
        List<Object[]> topValidationFailures =
                repository.topTextValues(supplierId, AnalyticsEvent.EventType.VALIDATION_FAILURE, PageRequest.of(0, 10));

        return Map.of(
                "averageOrderValue", aov == null ? 0 : aov,
                "aiExtractionFailures", extractionFailures,
                "validationFailures", validationFailures,
                "topProducts", toPairs(topProducts, "productId"),
                "repeatCustomers", toPairs(repeatCustomers, "retailerId"),
                "topAliases", toPairs(topAliases, "value"),
                "topValidationFailures", toPairs(topValidationFailures, "value")
        );
    }

    private List<Map<String, Object>> toPairs(List<Object[]> rows, String keyName) {
        return rows.stream()
                .map(r -> Map.of(keyName, r[0], "count", r[1]))
                .toList();
    }
}
