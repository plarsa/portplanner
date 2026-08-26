package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.PricingRule;

import java.util.List;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findBySeasonId(Long seasonId);
    Optional<PricingRule> findFirstBySeasonId(Long seasonId);
}
