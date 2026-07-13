package com.vyaparsetu.pricing.service;

import com.vyaparsetu.catalog.entity.Product;
import com.vyaparsetu.pricing.entity.CustomerPrice;
import com.vyaparsetu.pricing.repository.CustomerPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central price engine. ALL price resolution happens here in the backend — never in the LLM.
 *
 * Resolution order for the unit price of a product for a given retailer:
 *   1. Customer-specific price override (customer_prices)
 *   2. (Tier / offer pricing hooks — reserved for future rules)
 *   3. Product default selling price
 *
 * GST is then applied on top of the resolved unit price.
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CustomerPriceRepository customerPriceRepository;

    public PricingService(CustomerPriceRepository customerPriceRepository) {
        this.customerPriceRepository = customerPriceRepository;
    }

    /** Fully priced order line: resolved unit price, net line total and GST tax. */
    public record PricedLine(Long productId, String productName, BigDecimal quantity,
                             BigDecimal unitPrice, BigDecimal gstRate,
                             BigDecimal lineTotal, BigDecimal lineTax) {
    }

    /**
     * Resolve the effective unit price for a product/retailer pair.
     * Falls back to the product's default selling price when no override applies.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveUnitPrice(Product product, Long retailerId) {
        return customerPriceRepository
                .findBySupplierIdAndRetailerIdAndProductIdAndActiveTrue(
                        product.getSupplierId(), retailerId, product.getId())
                .map(CustomerPrice::getUnitPrice)
                .orElse(product.getSellingPrice());
    }

    /** Price a single line including GST, using the resolved unit price. */
    @Transactional(readOnly = true)
    public PricedLine priceLine(Product product, Long retailerId, BigDecimal quantity) {
        BigDecimal unitPrice = resolveUnitPrice(product, retailerId);
        BigDecimal lineTotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstRate = product.getGstRate() == null ? BigDecimal.ZERO : product.getGstRate();
        BigDecimal lineTax = lineTotal.multiply(gstRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new PricedLine(product.getId(), product.getName(), quantity,
                unitPrice, gstRate, lineTotal, lineTax);
    }
}
