package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.StoragePlacement;

import java.util.List;
import java.util.Optional;

public interface StoragePlacementRepository extends JpaRepository<StoragePlacement, Long> {
    List<StoragePlacement> findByYardIdOrderByYMetersAscXMetersAsc(Long yardId);
    Optional<StoragePlacement> findByBookingId(Long bookingId);
    void deleteByYardId(Long yardId);
}
