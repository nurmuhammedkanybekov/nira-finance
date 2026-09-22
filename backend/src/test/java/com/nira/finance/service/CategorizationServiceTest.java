package com.nira.finance.service;

import com.nira.finance.model.Category;
import com.nira.finance.model.Transaction;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.service.ai.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategorizationServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final CategorizationService service = new CategorizationService(categoryRepository, aiClient);

    private Category groceries;

    @BeforeEach
    void setUp() {
        groceries = new Category();
        groceries.setId(1L);
        groceries.setUserId(99L);
        groceries.setName("Groceries");
        when(categoryRepository.findByUserId(99L)).thenReturn(List.of(groceries));
    }

    @Test
    void matchesByMerchantNameWithoutCallingAi() {
        Transaction tx = new Transaction();
        tx.setMerchant("Groceries R Us");
        tx.setDescription("weekly shop");
        tx.setAmount(new BigDecimal("-45.20"));
        tx.setOccurredOn(LocalDate.now());

        Optional<Long> result = service.suggestCategory(99L, tx);

        assertEquals(Optional.of(1L), result);
        verifyNoInteractions(aiClient);
    }

    @Test
    void fallsBackToAiWhenNoRuleMatches() {
        Transaction tx = new Transaction();
        tx.setMerchant("Some Random Shop");
        tx.setDescription("misc purchase");
        tx.setAmount(new BigDecimal("-12.00"));
        tx.setOccurredOn(LocalDate.now());

        when(aiClient.categorize(anyString(), anyString(), anyList())).thenReturn(Optional.of(1L));

        Optional<Long> result = service.suggestCategory(99L, tx);

        assertEquals(Optional.of(1L), result);
        verify(aiClient).categorize(eq("misc purchase"), eq("Some Random Shop"), anyList());
    }
}
