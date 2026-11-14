package se.portplanner.service;

import org.junit.jupiter.api.Test;
import se.portplanner.exception.ConflictException;
import se.portplanner.model.RentalObject;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalRuleValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 9, 0);

    private final RentalRuleValidator validator = new RentalRuleValidator();

    private RentalObject object() {
        var o = new RentalObject();
        o.setName("Dagseglare");
        o.setBookable(true);
        o.setMaxDurationHours(8);
        o.setRequiresApproval(false);
        o.setMaxDaysAhead(30);
        return o;
    }

    private static LocalDateTime at(int day, int hour) {
        return LocalDateTime.of(2026, 5, day, hour, 0);
    }

    @Test
    void aValidBookingPasses() {
        assertDoesNotThrow(() -> validator.check(object(), at(2, 8), at(2, 12), NOW));
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThrows(ConflictException.class, () -> validator.check(object(), at(2, 12), at(2, 8), NOW));
    }

    @Test
    void aZeroLengthBookingIsRejected() {
        assertThrows(ConflictException.class, () -> validator.check(object(), at(2, 8), at(2, 8), NOW));
    }

    @Test
    void longerThanMaxDurationIsRejected() {
        assertThrows(ConflictException.class, () -> validator.check(object(), at(2, 8), at(2, 20), NOW));
    }

    @Test
    void aStartInThePastIsRejected() {
        assertThrows(ConflictException.class,
                () -> validator.check(object(), LocalDateTime.of(2026, 4, 30, 8, 0),
                        LocalDateTime.of(2026, 4, 30, 12, 0), NOW));
    }

    @Test
    void aStartBeyondMaxDaysAheadIsRejected() {
        var start = NOW.plusDays(31);
        assertThrows(ConflictException.class, () -> validator.check(object(), start, start.plusHours(4), NOW));
    }

    @Test
    void anObjectThatIsNotBookableIsRejected() {
        var o = object();
        o.setBookable(false);
        assertThrows(ConflictException.class, () -> validator.check(o, at(2, 8), at(2, 12), NOW));
    }

    @Test
    void aBookingAlmostTwiceTheMaxDurationIsRejected() {
        // 08:00-16:59 is 8h59m. Truncating to whole hours would read it as 8 and let it through.
        assertThrows(ConflictException.class,
                () -> validator.check(object(), at(2, 8), at(2, 16).plusMinutes(59), NOW));
    }

    @Test
    void aBookingExactlyAtMaxDurationPasses() {
        assertDoesNotThrow(() -> validator.check(object(), at(2, 8), at(2, 16), NOW));
    }
}
