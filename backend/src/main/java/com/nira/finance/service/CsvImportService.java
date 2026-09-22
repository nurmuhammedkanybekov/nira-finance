package com.nira.finance.service;

import com.nira.finance.exception.ForbiddenOperationException;
import com.nira.finance.model.Transaction;
import com.nira.finance.model.TransactionSource;
import com.nira.finance.repository.AccountRepository;
import com.nira.finance.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Expects a CSV with (at minimum) columns: date, description, amount.
 * Optional columns: merchant, currency. Header names are matched
 * case-insensitively so exports from most banks will work with minimal
 * cleanup.
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final int MAX_ROWS_PER_IMPORT = 20_000;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    };

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategorizationService categorizationService;

    public CsvImportService(TransactionRepository transactionRepository,
                             AccountRepository accountRepository,
                             CategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categorizationService = categorizationService;
    }

    public ImportResult importCsv(Long userId, Long accountId, MultipartFile file) throws IOException {
        accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("That account doesn't belong to you"));

        List<String> errors = new ArrayList<>();
        List<Transaction> saved = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                if (rowNum > MAX_ROWS_PER_IMPORT) {
                    errors.add("Stopped at row " + MAX_ROWS_PER_IMPORT + " - file too large for a single import; split it up.");
                    break;
                }
                try {
                    Transaction tx = parseRow(userId, accountId, record);
                    if (tx.getCategoryId() == null) {
                        categorizationService.suggestCategory(userId, tx).ifPresent(categoryId -> {
                            tx.setCategoryId(categoryId);
                            tx.setAiCategorized(true);
                        });
                    }
                    saved.add(transactionRepository.save(tx));
                } catch (Exception e) {
                    log.debug("Skipping row {} of import for user {}: {}", rowNum, userId, e.getMessage());
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        }

        log.info("CSV import for user {} account {}: {} imported, {} errors",
                userId, accountId, saved.size(), errors.size());
        return new ImportResult(saved.size(), errors);
    }

    private Transaction parseRow(Long userId, Long accountId, CSVRecord record) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setAccountId(accountId);
        tx.setSource(TransactionSource.CSV_IMPORT);
        tx.setAmount(new BigDecimal(get(record, "amount").replace(",", ".")));
        tx.setDescription(get(record, "description"));
        tx.setMerchant(hasColumn(record, "merchant") ? get(record, "merchant") : null);
        tx.setCurrency(hasColumn(record, "currency") ? get(record, "currency") : "EUR");
        tx.setOccurredOn(parseDate(get(record, "date")));
        return tx;
    }

    private String get(CSVRecord record, String column) {
        return record.get(column);
    }

    private boolean hasColumn(CSVRecord record, String column) {
        return record.isMapped(column) && !record.get(column).isBlank();
    }

    private LocalDate parseDate(String raw) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, format);
            } catch (Exception ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException("Unrecognized date format: " + raw);
    }

    public record ImportResult(int imported, List<String> errors) {}
}
