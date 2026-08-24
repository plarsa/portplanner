package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.Assignment;
import se.portplanner.model.AssignmentStatus;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByStatus(AssignmentStatus status);
    List<Assignment> findByBoatId(Long boatId);
    Optional<Assignment> findByBoatIdAndStatus(Long boatId, AssignmentStatus status);
    Optional<Assignment> findBySlipIdAndStatus(Long slipId, AssignmentStatus status);
    boolean existsBySlipIdAndStatus(Long slipId, AssignmentStatus status);
    void deleteBySlipId(Long slipId);
    List<Assignment> findByBoatOwnerIdAndStatus(Long personId, AssignmentStatus status);
}
