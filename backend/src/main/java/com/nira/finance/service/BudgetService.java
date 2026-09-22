package com.nira.finance.service;

import com.nira.finance.exception.ForbiddenOperationException;
import com.nira.finance.model.Budget;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.BudgetRepository;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                          CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Budget setBudget(Long userId, Long categoryId, YearMonth month, BigDecimal limit) {
        categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("That category doesn't belong to you"));

        LocalDate monthStart = month.atDay(1);
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, monthStart).stream()
                .filter(b -> b.getCategoryId().equals(categoryId))
                .findFirst()
                .orElseGet(Budget::new);

        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setMonth(monthStart);
        budget.setLimitAmount(limit);
        return budgetRepository.save(budget);
    }

    /** Budget limit vs. actual spend per category for a given month. */
    public List<BudgetStatus> statusForMonth(Long userId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(userId, monthStart);
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndOccurredOnBetween(userId, monthStart, monthEnd);

        Map<Long, BigDecimal> spendByCategory = transactions.stream()
                .filter(t -> t.getCategoryId() != null && t.getAmount().signum() < 0)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)
                ));

        return budgets.stream()
                .map(b -> new BudgetStatus(
                        b.getCategoryId(),
                        b.getLimitAmount(),
                        spendByCategory.getOrDefault(b.getCategoryId(), BigDecimal.ZERO)
                ))
                .collect(Collectors.toList());
    }

    public record BudgetStatus(Long categoryId, BigDecimal limit, BigDecimal spent) {
        public BigDecimal remaining() { return limit.subtract(spent); }
        public boolean isOverBudget() { return spent.compareTo(limit) > 0; }
    }
}
