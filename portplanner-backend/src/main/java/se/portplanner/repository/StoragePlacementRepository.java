package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.portplanner.model.StoragePlacement;

import java.util.List;
import java.util.Optional;

public interface StoragePlacementRepository extends JpaRepository<StoragePlacement, Long> {
    @Query("SELECT p FROM StoragePlacement p WHERE p.yard.id = :yardId ORDER BY p.yMeters ASC, p.xMeters ASC")
    List<StoragePlacement> findByYardIdOrderByYMetersAscXMetersAsc(@Param("yardId") Long yardId);
    Optional<StoragePlacement> findByBookingId(Long bookingId);
    void deleteByYardId(Long yardId);
}
