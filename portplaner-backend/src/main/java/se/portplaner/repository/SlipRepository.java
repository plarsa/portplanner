package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.Slip;
import se.portplaner.model.SlipStatus;

import java.util.List;

public interface SlipRepository extends JpaRepository<Slip, Long> {
    List<Slip> findByDockId(Long dockId);
    List<Slip> findByStatus(SlipStatus status);
}
