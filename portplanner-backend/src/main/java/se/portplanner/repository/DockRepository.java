package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.Dock;

public interface DockRepository extends JpaRepository<Dock, Long> {
}
