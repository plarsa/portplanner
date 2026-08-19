package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.Boat;

import java.util.List;

public interface BoatRepository extends JpaRepository<Boat, Long> {
    List<Boat> findByOwnerId(Long ownerId);
    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
}
