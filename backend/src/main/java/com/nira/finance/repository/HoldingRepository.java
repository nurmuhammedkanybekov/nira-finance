package com.nira.finance.repository;

import com.nira.finance.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUserId(Long userId);
    List<Holding> findByAccountId(Long accountId);
    Optional<Holding> findByIdAndUserId(Long id, Long userId);
}
