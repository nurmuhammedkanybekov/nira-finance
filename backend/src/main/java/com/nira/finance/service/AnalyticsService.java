package com.nira.finance.service;

import com.nira.finance.dto.CategorySummary;
import com.nira.finance.model.Category;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public AnalyticsService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<CategorySummary> spendByCategory(Long userId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndOccurredOnBetween(userId, from, to);
        Map<Long, Category> categoriesById = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<String, BigDecimal> totals = transactions.stream()
                .filter(t -> t.getAmount().signum() < 0)
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryId() != null && categoriesById.containsKey(t.getCategoryId())
                                ? categoriesById.get(t.getCategoryId()).getName() : "Uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)
                ));

        return totals.entrySet().stream()
                .map(e -> new CategorySummary(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());
    }

    public BigDecimal netForRange(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.findByUserIdAndOccurredOnBetween(userId, from, to).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
