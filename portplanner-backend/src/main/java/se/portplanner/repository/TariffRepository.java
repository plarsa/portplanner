package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.portplanner.model.Tariff;

import java.time.LocalDate;
import java.util.List;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findAllByOrderByCategoryAscValidFromDesc();

    @Query("SELECT t FROM Tariff t WHERE t.category = :category " +
           "AND t.validFrom <= :date AND (t.validTo IS NULL OR t.validTo >= :date)")
    List<Tariff> findActiveByCategory(@Param("category") String category, @Param("date") LocalDate date);
}
