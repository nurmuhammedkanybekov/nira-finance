package com.nira.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class HoldingRequest {
    @NotNull
    private Long accountId;

    @NotBlank
    private String ticker;

    @NotNull
    @DecimalMin(value = "0.000001", message = "Shares must be greater than zero")
    private BigDecimal shares;

    @NotNull
    @DecimalMin(value = "0", message = "Cost basis can't be negative")
    private BigDecimal costBasis;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public BigDecimal getShares() { return shares; }
    public void setShares(BigDecimal shares) { this.shares = shares; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
}
