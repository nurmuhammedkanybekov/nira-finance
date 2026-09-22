package com.nira.finance.service;

import com.nira.finance.model.Category;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.service.ai.AiClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Categorizes transactions. Tries cheap rule-based matching on the merchant/
 * description first (fast, free, deterministic) and only falls back to the
 * LLM for anything it doesn't recognize.
 */
@Service
public class CategorizationService {

    private final CategoryRepository categoryRepository;
    private final AiClient aiClient;

    public CategorizationService(CategoryRepository categoryRepository, AiClient aiClient) {
        this.categoryRepository = categoryRepository;
        this.aiClient = aiClient;
    }

    public Optional<Long> suggestCategory(Long userId, Transaction tx) {
        List<Category> categories = categoryRepository.findByUserId(userId);
        if (categories.isEmpty()) {
            return Optional.empty();
        }

        Optional<Long> ruleMatch = ruleBasedMatch(tx, categories);
        if (ruleMatch.isPresent()) {
            return ruleMatch;
        }

        return aiClient.categorize(tx.getDescription(), tx.getMerchant(), categories);
    }

    private Optional<Long> ruleBasedMatch(Transaction tx, List<Category> categories) {
        String haystack = ((tx.getMerchant() == null ? "" : tx.getMerchant()) + " " +
                (tx.getDescription() == null ? "" : tx.getDescription())).toLowerCase();

        for (Category category : categories) {
            if (haystack.contains(category.getName().toLowerCase())) {
                return Optional.of(category.getId());
            }
        }
        return Optional.empty();
    }
}
