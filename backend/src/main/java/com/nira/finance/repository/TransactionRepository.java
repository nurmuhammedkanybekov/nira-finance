package com.nira.finance.repository;

import com.nira.finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByOccurredOnDesc(Long userId);
    List<Transaction> findByUserIdAndOccurredOnBetween(Long userId, LocalDate from, LocalDate to);
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
