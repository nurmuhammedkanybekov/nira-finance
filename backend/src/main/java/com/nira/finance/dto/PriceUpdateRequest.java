package com.nira.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PriceUpdateRequest {
    @NotNull
    @DecimalMin(value = "0", message = "Price can't be negative")
    private BigDecimal currentPrice;

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
}
