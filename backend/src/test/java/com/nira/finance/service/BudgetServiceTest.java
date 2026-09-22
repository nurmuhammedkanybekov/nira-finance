package com.nira.finance.service;

import com.nira.finance.model.Budget;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.BudgetRepository;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetServiceTest {

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final BudgetService service = new BudgetService(budgetRepository, transactionRepository, categoryRepository);

    @Test
    void flagsOverBudgetWhenSpendExceedsLimit() {
        Long userId = 1L;
        Long groceriesCategory = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(groceriesCategory);
        budget.setMonth(month.atDay(1));
        budget.setLimitAmount(new BigDecimal("300.00"));

        when(budgetRepository.findByUserIdAndMonth(userId, month.atDay(1))).thenReturn(List.of(budget));
        when(transactionRepository.findByUserIdAndOccurredOnBetween(userId, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(
                        expense(userId, groceriesCategory, "-120.00", month.atDay(3)),
                        expense(userId, groceriesCategory, "-250.00", month.atDay(20))
                ));

        List<BudgetService.BudgetStatus> statuses = service.statusForMonth(userId, month);

        assertEquals(1, statuses.size());
        BudgetService.BudgetStatus status = statuses.get(0);
        assertEquals(new BigDecimal("370.00"), status.spent());
        assertTrue(status.isOverBudget());
        assertEquals(new BigDecimal("-70.00"), status.remaining());
    }

    @Test
    void incomeTransactionsDoNotCountAsSpend() {
        Long userId = 1L;
        Long salaryCategory = 20L;
        YearMonth month = YearMonth.of(2026, 9);

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(salaryCategory);
        budget.setMonth(month.atDay(1));
        budget.setLimitAmount(new BigDecimal("100.00"));

        when(budgetRepository.findByUserIdAndMonth(userId, month.atDay(1))).thenReturn(List.of(budget));
        when(transactionRepository.findByUserIdAndOccurredOnBetween(userId, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(expense(userId, salaryCategory, "2000.00", month.atDay(1)))); // positive = income

        List<BudgetService.BudgetStatus> statuses = service.statusForMonth(userId, month);

        assertEquals(BigDecimal.ZERO, statuses.get(0).spent());
        assertFalse(statuses.get(0).isOverBudget());
    }

    private Transaction expense(Long userId, Long categoryId, String amount, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setCategoryId(categoryId);
        tx.setAmount(new BigDecimal(amount));
        tx.setOccurredOn(date);
        return tx;
    }
}
