package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.RentalBooking;
import se.portplanner.model.RentalBookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {

    /**
     * Bookings in the given states that overlap the half-open interval. Two half-open intervals
     * overlap exactly when each starts before the other ends, so a booking that ends at the moment
     * another starts is not an overlap.
     */
    List<RentalBooking> findByObjectIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            Long objectId, Collection<RentalBookingStatus> statuses, LocalDateTime before, LocalDateTime after);

    List<RentalBooking> findByPersonIdOrderByStartAtDesc(Long personId);

    boolean existsByPersonId(Long personId);
}
