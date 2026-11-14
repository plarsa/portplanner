package se.portplanner.service;

import org.springframework.stereotype.Component;
import se.portplanner.exception.ConflictException;
import se.portplanner.repository.RentalBookingRepository;

/** Keeps a person with rental history from being deleted out from under their bookings. */
@Component
public class RentalPersonDeletionCheck implements PersonDeletionCheck {

    private final RentalBookingRepository bookings;

    public RentalPersonDeletionCheck(RentalBookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public void check(Long personId) {
        if (bookings.existsByPersonId(personId)) {
            throw new ConflictException("The person has rental bookings. Cancel them first");
        }
    }
}
