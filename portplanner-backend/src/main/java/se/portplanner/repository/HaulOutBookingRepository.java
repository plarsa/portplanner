package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.HaulOutBooking;
import se.portplanner.model.HaulOutBookingStatus;

import java.util.List;

public interface HaulOutBookingRepository extends JpaRepository<HaulOutBooking, Long> {
    List<HaulOutBooking> findBySlotIdOrderByRequestedAtAsc(Long slotId);
    List<HaulOutBooking> findBySlotSeasonIdAndStatusIn(Long seasonId, List<HaulOutBookingStatus> statuses);
    int countBySlotIdAndStatusNot(Long slotId, HaulOutBookingStatus status);
    boolean existsByBoatIdAndSlotSeasonIdAndStatusNot(Long boatId, Long seasonId, HaulOutBookingStatus status);
}
