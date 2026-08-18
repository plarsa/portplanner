package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.Dock;

public interface DockRepository extends JpaRepository<Dock, Long> {
}
