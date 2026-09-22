package com.nira.finance.controller;

import com.nira.finance.dto.ChatRequest;
import com.nira.finance.dto.ChatResponse;
import com.nira.finance.security.CurrentUser;
import com.nira.finance.service.ai.FinanceIndexingService;
import com.nira.finance.service.ai.RagChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final FinanceIndexingService indexingService;

    public ChatController(RagChatService ragChatService, FinanceIndexingService indexingService) {
        this.ragChatService = ragChatService;
        this.indexingService = indexingService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        return ragChatService.ask(CurrentUser.id(), request.getQuestion());
    }

    /** Rebuild the RAG index for the current user - call after importing/editing transactions. */
    @PostMapping("/reindex")
    public ResponseEntity<Void> reindex() {
        indexingService.reindexUser(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
