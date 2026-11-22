package com.expiryguard.markdown.repository;

import com.expiryguard.markdown.model.MarkdownRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkdownRuleRepository extends JpaRepository<MarkdownRule, Long> {
    List<MarkdownRule> findByActiveTrueOrderByDaysBeforeExpiryAsc();
    boolean existsByDaysBeforeExpiry(int daysBeforeExpiry);
}
