package com.nira.finance.controller;

import com.nira.finance.security.CurrentUser;
import com.nira.finance.service.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<Void> set(@RequestParam Long categoryId, @RequestParam String month, @RequestParam BigDecimal limit) {
        budgetService.setBudget(CurrentUser.id(), categoryId, YearMonth.parse(month), limit);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<BudgetService.BudgetStatus> status(@RequestParam String month) {
        return budgetService.statusForMonth(CurrentUser.id(), YearMonth.parse(month));
    }
}
