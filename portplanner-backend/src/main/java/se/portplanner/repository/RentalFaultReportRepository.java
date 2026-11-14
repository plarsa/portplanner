package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.RentalFaultReport;

import java.util.List;

public interface RentalFaultReportRepository extends JpaRepository<RentalFaultReport, Long> {

    List<RentalFaultReport> findByResolvedAtIsNullOrderByReportedAtDesc();

    List<RentalFaultReport> findByResolvedAtIsNotNullOrderByReportedAtDesc();

    List<RentalFaultReport> findByBookingObjectIdOrderByReportedAtDesc(Long objectId);
}
