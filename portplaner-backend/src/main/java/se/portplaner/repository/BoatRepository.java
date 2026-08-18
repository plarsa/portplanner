package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.Boat;

import java.util.List;

public interface BoatRepository extends JpaRepository<Boat, Long> {
    List<Boat> findByOwnerId(Long ownerId);
}
