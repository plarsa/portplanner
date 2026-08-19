package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.Slip;
import se.portplanner.model.SlipStatus;

import java.util.List;

public interface SlipRepository extends JpaRepository<Slip, Long> {
    List<Slip> findByDockId(Long dockId);
    List<Slip> findByStatus(SlipStatus status);
}
