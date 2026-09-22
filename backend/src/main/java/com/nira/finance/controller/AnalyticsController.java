package com.nira.finance.controller;

import com.nira.finance.dto.CategorySummary;
import com.nira.finance.security.CurrentUser;
import com.nira.finance.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/spend-by-category")
    public List<CategorySummary> spendByCategory(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return analyticsService.spendByCategory(CurrentUser.id(), from, to);
    }

    @GetMapping("/net")
    public BigDecimal net(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return analyticsService.netForRange(CurrentUser.id(), from, to);
    }
}
