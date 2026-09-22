package com.nira.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for a holding, with valuation derived server-side so the
 * frontend never has to duplicate the "no live price yet -> fall back to
 * cost basis" rule.
 */
public record HoldingView(
        Long id,
        Long accountId,
        String ticker,
        BigDecimal shares,
        BigDecimal costBasis,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal gainLoss,
        BigDecimal gainLossPercent,
        boolean priceIsEstimated,
        Instant updatedAt
) {}
