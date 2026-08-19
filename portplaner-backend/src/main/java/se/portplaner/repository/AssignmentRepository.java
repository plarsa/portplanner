package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.Assignment;
import se.portplaner.model.AssignmentStatus;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByStatus(AssignmentStatus status);
    List<Assignment> findByBoatId(Long boatId);
    Optional<Assignment> findByBoatIdAndStatus(Long boatId, AssignmentStatus status);
    boolean existsBySlipIdAndStatus(Long slipId, AssignmentStatus status);
    void deleteBySlipId(Long slipId);
}
