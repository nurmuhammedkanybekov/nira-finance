package com.nira.finance.controller;

import com.nira.finance.dto.TransactionRequest;
import com.nira.finance.model.Transaction;
import com.nira.finance.security.CurrentUser;
import com.nira.finance.service.CsvImportService;
import com.nira.finance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CsvImportService csvImportService;

    public TransactionController(TransactionService transactionService, CsvImportService csvImportService) {
        this.transactionService = transactionService;
        this.csvImportService = csvImportService;
    }

    @PostMapping
    public Transaction create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(CurrentUser.id(), request);
    }

    @GetMapping
    public List<Transaction> list(@RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to) {
        Long userId = CurrentUser.id();
        if (from != null && to != null) {
            return transactionService.listForRange(userId, from, to);
        }
        return transactionService.listForUser(userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public CsvImportService.ImportResult importCsv(@RequestParam Long accountId,
                                                     @RequestParam MultipartFile file) throws IOException {
        return csvImportService.importCsv(CurrentUser.id(), accountId, file);
    }
}
