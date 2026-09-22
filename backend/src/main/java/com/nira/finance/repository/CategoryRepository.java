package com.nira.finance.repository;

import com.nira.finance.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId);
    Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
