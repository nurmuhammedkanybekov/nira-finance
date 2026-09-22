package com.nira.finance.service;

import com.nira.finance.dto.HoldingRequest;
import com.nira.finance.dto.HoldingView;
import com.nira.finance.exception.ForbiddenOperationException;
import com.nira.finance.exception.ResourceNotFoundException;
import com.nira.finance.model.Holding;
import com.nira.finance.repository.AccountRepository;
import com.nira.finance.repository.HoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);
    private static final int MONEY_SCALE = 2;

    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;

    public HoldingService(HoldingRepository holdingRepository, AccountRepository accountRepository) {
        this.holdingRepository = holdingRepository;
        this.accountRepository = accountRepository;
    }

    public HoldingView create(Long userId, HoldingRequest request) {
        accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ForbiddenOperationException("That account doesn't belong to you"));

        Holding holding = new Holding();
        holding.setUserId(userId);
        holding.setAccountId(request.getAccountId());
        holding.setTicker(request.getTicker().toUpperCase());
        holding.setShares(request.getShares());
        holding.setCostBasis(request.getCostBasis());
        holding.setUpdatedAt(Instant.now());

        Holding saved = holdingRepository.save(holding);
        log.info("Created holding {} ({}) for user {}", saved.getId(), saved.getTicker(), userId);
        return toView(saved);
    }

    public List<HoldingView> listForUser(Long userId) {
        return holdingRepository.findByUserId(userId).stream().map(this::toView).toList();
    }

    public HoldingView updatePrice(Long userId, Long holdingId, BigDecimal currentPrice) {
        Holding holding = holdingRepository.findByIdAndUserId(holdingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding " + holdingId + " not found"));
        holding.setCurrentPrice(currentPrice);
        holding.setUpdatedAt(Instant.now());
        return toView(holdingRepository.save(holding));
    }

    public void delete(Long userId, Long holdingId) {
        Holding holding = holdingRepository.findByIdAndUserId(holdingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding " + holdingId + " not found"));
        holdingRepository.delete(holding);
        log.info("Deleted holding {} for user {}", holdingId, userId);
    }

    public PortfolioSummary summaryForUser(Long userId) {
        List<HoldingView> holdings = listForUser(userId);
        BigDecimal totalCost = holdings.stream().map(HoldingView::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = holdings.stream().map(HoldingView::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGainLoss = totalValue.subtract(totalCost);
        BigDecimal totalGainLossPercent = percentChange(totalCost, totalGainLoss);
        return new PortfolioSummary(totalCost, totalValue, totalGainLoss, totalGainLossPercent);
    }

    private HoldingView toView(Holding h) {
        boolean priceIsEstimated = h.getCurrentPrice() == null;
        BigDecimal marketValue = priceIsEstimated
                ? h.getCostBasis()
                : h.getShares().multiply(h.getCurrentPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal gainLoss = marketValue.subtract(h.getCostBasis());
        BigDecimal gainLossPercent = percentChange(h.getCostBasis(), gainLoss);

        return new HoldingView(
                h.getId(), h.getAccountId(), h.getTicker(), h.getShares(), h.getCostBasis(),
                h.getCurrentPrice(), marketValue, gainLoss, gainLossPercent, priceIsEstimated, h.getUpdatedAt());
    }

    private BigDecimal percentChange(BigDecimal base, BigDecimal delta) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        return delta.divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record PortfolioSummary(BigDecimal totalCostBasis, BigDecimal totalMarketValue,
                                    BigDecimal totalGainLoss, BigDecimal totalGainLossPercent) {}
}
