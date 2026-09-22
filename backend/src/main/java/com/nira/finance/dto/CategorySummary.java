package com.nira.finance.dto;

import java.math.BigDecimal;

public class CategorySummary {
    private String categoryName;
    private BigDecimal total;

    public CategorySummary(String categoryName, BigDecimal total) {
        this.categoryName = categoryName;
        this.total = total;
    }
    public String getCategoryName() { return categoryName; }
    public BigDecimal getTotal() { return total; }
}
