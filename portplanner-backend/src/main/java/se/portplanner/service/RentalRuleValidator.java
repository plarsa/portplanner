package se.portplanner.service;

import org.springframework.stereotype.Component;
import se.portplanner.exception.ConflictException;
import se.portplanner.model.RentalObject;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Checks a requested interval against the object's own rules (REN-3, REN-5). Overlap with another
 * booking is not checked here: that needs the repository and belongs to the booking service.
 *
 * <p>A rule breach is a {@link ConflictException}, which the exception handler already answers 409
 * on. PolicyViolationException is not used, since that answers 403 and this is not a rights problem.
 */
@Component
public class RentalRuleValidator {

    /** {@code now} is passed in so the caller's clock is the one under test. */
    public void check(RentalObject object, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (!object.isBookable()) {
            throw new ConflictException("The object " + object.getName() + " is not open for booking");
        }
        if (start == null || end == null) {
            throw new ConflictException("A booking needs both a start and an end time");
        }
        if (!end.isAfter(start)) {
            throw new ConflictException("The booking must end after it starts");
        }
        var hours = Duration.between(start, end).toHours();
        if (hours > object.getMaxDurationHours()) {
            throw new ConflictException("The booking is " + hours + " hours long, and the object allows at most "
                    + object.getMaxDurationHours());
        }
        if (start.isBefore(now)) {
            throw new ConflictException("The booking cannot start in the past");
        }
        if (start.isAfter(now.plusDays(object.getMaxDaysAhead()))) {
            throw new ConflictException("The object can be booked at most " + object.getMaxDaysAhead()
                    + " days ahead");
        }
    }
}
