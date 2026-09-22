package com.nira.finance.service;

import com.nira.finance.dto.TransactionRequest;
import com.nira.finance.exception.ForbiddenOperationException;
import com.nira.finance.exception.ResourceNotFoundException;
import com.nira.finance.model.Transaction;
import com.nira.finance.model.TransactionSource;
import com.nira.finance.repository.AccountRepository;
import com.nira.finance.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategorizationService categorizationService;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               CategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categorizationService = categorizationService;
    }

    public Transaction create(Long userId, TransactionRequest request) {
        accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ForbiddenOperationException("That account doesn't belong to you"));

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setAccountId(request.getAccountId());
        tx.setCategoryId(request.getCategoryId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setDescription(request.getDescription());
        tx.setMerchant(request.getMerchant());
        tx.setOccurredOn(request.getOccurredOn());
        tx.setSource(TransactionSource.MANUAL);

        // If the user didn't pick a category, let the AI take a first pass at it.
        if (tx.getCategoryId() == null) {
            categorizationService.suggestCategory(userId, tx).ifPresent(categoryId -> {
                tx.setCategoryId(categoryId);
                tx.setAiCategorized(true);
            });
        }

        Transaction saved = transactionRepository.save(tx);
        log.info("Created transaction {} for user {} ({} {})", saved.getId(), userId, saved.getAmount(), saved.getCurrency());
        return saved;
    }

    public List<Transaction> listForUser(Long userId) {
        return transactionRepository.findByUserIdOrderByOccurredOnDesc(userId);
    }

    public List<Transaction> listForRange(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.findByUserIdAndOccurredOnBetween(userId, from, to);
    }

    public void delete(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + transactionId + " not found"));
        if (!tx.getUserId().equals(userId)) {
            log.warn("User {} attempted to delete transaction {} owned by user {}", userId, transactionId, tx.getUserId());
            throw new ForbiddenOperationException("That transaction doesn't belong to you");
        }
        transactionRepository.delete(tx);
        log.info("Deleted transaction {} for user {}", transactionId, userId);
    }
}
