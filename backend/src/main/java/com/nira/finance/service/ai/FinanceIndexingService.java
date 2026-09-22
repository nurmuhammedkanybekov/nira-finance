package com.nira.finance.service.ai;

import com.nira.finance.model.Category;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the text chunks that get embedded for RAG: one chunk per
 * month-of-transactions per category, plus one chunk per large individual
 * transaction. Re-run this whenever transactions change meaningfully
 * (after an import, or on a schedule) - it's cheap to fully rebuild for a
 * single user's data at this scale.
 */
@Service
public class FinanceIndexingService {

    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("300");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final FinanceEmbeddingRepository embeddingRepository;
    private final AiClient aiClient;

    public FinanceIndexingService(TransactionRepository transactionRepository,
                                   CategoryRepository categoryRepository,
                                   FinanceEmbeddingRepository embeddingRepository,
                                   AiClient aiClient) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.embeddingRepository = embeddingRepository;
        this.aiClient = aiClient;
    }

    public void reindexUser(Long userId) {
        embeddingRepository.deleteAllForUser(userId);

        List<Transaction> transactions = transactionRepository.findByUserIdOrderByOccurredOnDesc(userId);
        Map<Long, String> categoryNames = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        indexMonthlyCategorySummaries(userId, transactions, categoryNames);
        indexLargeTransactions(userId, transactions, categoryNames);
    }

    private void indexMonthlyCategorySummaries(Long userId, List<Transaction> transactions,
                                                Map<Long, String> categoryNames) {
        Map<String, BigDecimal> totals = transactions.stream()
                .filter(t -> t.getAmount().signum() < 0)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getOccurredOn()) + "|" + categoryNames.getOrDefault(t.getCategoryId(), "Uncategorized"),
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)
                ));

        totals.forEach((key, total) -> {
            String[] parts = key.split("\\|", 2);
            YearMonth month = YearMonth.parse(parts[0]);
            String category = parts[1];
            String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();

            String content = "In %s, the user spent %s on %s.".formatted(monthName, total, category);
            embeddingRepository.insert(userId, "MONTHLY_SUMMARY", null, content, aiClient.embed(content));
        });
    }

    private void indexLargeTransactions(Long userId, List<Transaction> transactions,
                                         Map<Long, String> categoryNames) {
        transactions.stream()
                .filter(t -> t.getAmount().abs().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0)
                .forEach(t -> {
                    String direction = t.getAmount().signum() < 0 ? "spent" : "received";
                    String category = categoryNames.getOrDefault(t.getCategoryId(), "Uncategorized");
                    String content = "On %s, the user %s %s (%s) at %s, category %s.".formatted(
                            t.getOccurredOn(), direction, t.getAmount().abs(), t.getCurrency(),
                            t.getMerchant() != null ? t.getMerchant() : "an unspecified merchant", category);
                    embeddingRepository.insert(userId, "TRANSACTION", t.getId(), content, aiClient.embed(content));
                });
    }
}
