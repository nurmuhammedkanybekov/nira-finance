package com.nira.finance.controller;

import com.nira.finance.dto.HoldingRequest;
import com.nira.finance.dto.HoldingView;
import com.nira.finance.dto.PriceUpdateRequest;
import com.nira.finance.security.CurrentUser;
import com.nira.finance.service.HoldingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping
    public List<HoldingView> list() {
        return holdingService.listForUser(CurrentUser.id());
    }

    @GetMapping("/summary")
    public HoldingService.PortfolioSummary summary() {
        return holdingService.summaryForUser(CurrentUser.id());
    }

    @PostMapping
    public HoldingView create(@Valid @RequestBody HoldingRequest request) {
        return holdingService.create(CurrentUser.id(), request);
    }

    @PutMapping("/{id}/price")
    public HoldingView updatePrice(@PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        return holdingService.updatePrice(CurrentUser.id(), id, request.getCurrentPrice());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holdingService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
