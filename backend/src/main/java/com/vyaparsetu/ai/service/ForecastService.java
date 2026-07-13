package com.vyaparsetu.ai.service;

import com.vyaparsetu.ai.repository.DemandRepository;
import com.vyaparsetu.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Demand forecasting (Phase 3). Uses a simple moving-average over recent sales
 * to estimate daily demand, days-to-stockout and a suggested next purchase date.
 */
@Service
public class ForecastService {

    private static final int WINDOW_DAYS = 30;

    private final DemandRepository demandRepository;
    private final UserService userService;

    public ForecastService(DemandRepository demandRepository, UserService userService) {
        this.demandRepository = demandRepository;
        this.userService = userService;
    }

    public record Forecast(Long productId, BigDecimal currentQty, BigDecimal dailyDemand,
                           Integer daysToStockout, LocalDate suggestedPurchaseDate,
                           BigDecimal suggestedQty) {
    }

    @Transactional(readOnly = true)
    public List<Forecast> forecast() {
        Long rid = userService.currentRetailerId();
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
        return demandRepository.demand(rid, since).stream()
                .map(this::toForecast)
                .toList();
    }

    private Forecast toForecast(DemandRepository.DemandRow row) {
        BigDecimal sold = row.getSold() != null ? row.getSold() : BigDecimal.ZERO;
        BigDecimal currentQty = row.getCurrentQty() != null ? row.getCurrentQty() : BigDecimal.ZERO;
        BigDecimal dailyDemand = sold.divide(BigDecimal.valueOf(WINDOW_DAYS), 3, RoundingMode.HALF_UP);

        Integer daysToStockout = null;
        LocalDate suggestedDate = null;
        BigDecimal suggestedQty = BigDecimal.ZERO;
        if (dailyDemand.signum() > 0) {
            int days = currentQty.divide(dailyDemand, 0, RoundingMode.FLOOR).intValue();
            daysToStockout = days;
            // suggest reordering 3 days before stockout, enough for the next 15 days
            suggestedDate = LocalDate.now(ZoneOffset.UTC).plusDays(Math.max(0, days - 3));
            suggestedQty = dailyDemand.multiply(BigDecimal.valueOf(15)).setScale(0, RoundingMode.CEILING);
        }
        return new Forecast(row.getProductId(), currentQty, dailyDemand,
                daysToStockout, suggestedDate, suggestedQty);
    }
}
