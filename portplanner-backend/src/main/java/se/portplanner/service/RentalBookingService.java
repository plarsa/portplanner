package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.RentalAdminBookingRequest;
import se.portplanner.dto.RentalBookingResponse;
import se.portplanner.dto.RentalBusyIntervalResponse;
import se.portplanner.dto.RentalFaultReportResponse;
import se.portplanner.dto.RentalReturnRequest;
import se.portplanner.exception.ConflictException;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.RentalBooking;
import se.portplanner.model.RentalBookingStatus;
import se.portplanner.model.RentalFaultReport;
import se.portplanner.repository.PersonRepository;
import se.portplanner.repository.RentalBookingRepository;
import se.portplanner.repository.RentalFaultReportRepository;
import se.portplanner.repository.RentalObjectRepository;

import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

/** Booking, cancelling and reading an object's calendar. */
@Service
@Transactional
public class RentalBookingService {

    private final RentalObjectRepository objects;
    private final RentalBookingRepository bookings;
    private final PersonRepository persons;
    private final RentalRuleValidator rules;
    private final AuditService auditService;
    private final RentalFaultReportRepository faults;

    public RentalBookingService(RentalObjectRepository objects, RentalBookingRepository bookings,
                                PersonRepository persons, RentalRuleValidator rules,
                                AuditService auditService, RentalFaultReportRepository faults) {
        this.objects = objects;
        this.bookings = bookings;
        this.persons = persons;
        this.rules = rules;
        this.auditService = auditService;
        this.faults = faults;
    }

    /** REN-4: the times only, never who holds them. */
    @Transactional(readOnly = true)
    public List<RentalBusyIntervalResponse> busyIntervals(Long objectId, LocalDateTime from, LocalDateTime to) {
        objects.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental object " + objectId + " was not found"));
        return bookings.findByObjectIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        objectId, RentalBookingStatus.holding(), to, from).stream()
                .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                .map(b -> new RentalBusyIntervalResponse(b.getStartAt(), b.getEndAt()))
                .toList();
    }

    /**
     * A new booking for a member. It joins the caller's transaction on purpose: the row lock taken
     * in {@link #create} is held until that transaction commits, which is what serialises competing
     * requests. Asking for a separate transaction would suspend the caller's while keeping its
     * connection, so every booking would hold two and a burst the size of the pool would deadlock.
     */
    @Transactional
    public RentalBookingResponse book(Long objectId, Long personId, LocalDateTime start, LocalDateTime end) {
        return create(objectId, personId, start, end, false);
    }

    /** REN-12: an administrator's booking skips approval but takes the same lock and checks. */
    @Transactional
    public RentalBookingResponse bookForPerson(RentalAdminBookingRequest req) {
        return create(req.objectId(), req.personId(), req.startAt(), req.endAt(), true);
    }

    /** REN-10: everything booked on an object in the interval, in whatever state. */
    @Transactional(readOnly = true)
    public List<RentalBookingResponse> findByObject(Long objectId, LocalDateTime from, LocalDateTime to) {
        return bookings.findByObjectIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(objectId, to, from)
                .stream().map(RentalBookingResponse::from).toList();
    }

    /** REN-6: the decision on a booking that has been holding its time since it was made. */
    public RentalBookingResponse approve(Long id) {
        return decide(id, RentalBookingStatus.CONFIRMED, "APPROVED", "approved");
    }

    public RentalBookingResponse reject(Long id) {
        return decide(id, RentalBookingStatus.REJECTED, "REJECTED", "rejected");
    }

    private RentalBookingResponse decide(Long id, RentalBookingStatus outcome,
                                         String event, String word) {
        var booking = bookings.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental booking " + id + " was not found"));
        if (booking.getStatus() != RentalBookingStatus.PENDING) {
            throw new ConflictException("Only a pending booking can be " + word + "; this one is "
                    + booking.getStatus().name().toLowerCase());
        }
        booking.setStatus(outcome);
        var saved = bookings.save(booking);
        auditService.log(event, "RENTAL_BOOKING", id,
                "Rental booking " + word + ": " + booking.getObject().getName() + " " + booking.getStartAt());
        return RentalBookingResponse.from(saved);
    }

    private RentalBookingResponse create(Long objectId, Long personId, LocalDateTime start,
                                         LocalDateTime end, boolean skipApproval) {
        var object = objects.findByIdForUpdate(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental object " + objectId + " was not found"));
        var person = persons.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person " + personId + " was not found"));

        rules.check(object, start, end, LocalDateTime.now());

        var overlapping = bookings.findByObjectIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                objectId, RentalBookingStatus.holding(), end, start);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("The object is already booked during that time");
        }

        var booking = new RentalBooking();
        booking.setObject(object);
        booking.setPerson(person);
        booking.setStartAt(start);
        booking.setEndAt(end);
        booking.setStatus(!skipApproval && object.isRequiresApproval()
                ? RentalBookingStatus.PENDING
                : RentalBookingStatus.CONFIRMED);
        var saved = bookings.save(booking);

        auditService.log("CREATED", "RENTAL_BOOKING", saved.getId(),
                "Rental booked: " + object.getName() + " " + start + " to " + end
                        + " for " + person.getFirstName() + " " + person.getLastName());
        return RentalBookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RentalBookingResponse> findByPerson(Long personId) {
        return bookings.findByPersonIdOrderByStartAtDesc(personId).stream()
                .map(RentalBookingResponse::from).toList();
    }

    /**
     * REN-7: a member cancels their own booking up to its start time. {@code personId} is null for
     * an administrator, who may cancel anyone's (REN-10); for a member a booking that is not theirs
     * is reported as missing rather than forbidden, so the API does not confirm it exists (REN-14).
     */
    public void cancel(Long bookingId, Long personId) {
        var booking = ownedOrThrow(bookingId, personId);
        // Not holding(): that includes RETURNED, and cancelling a returned booking would wipe the
        // record of the return while leaving returnedAt and any fault report behind.
        if (booking.getStatus() != RentalBookingStatus.PENDING
                && booking.getStatus() != RentalBookingStatus.CONFIRMED) {
            throw new ConflictException("The booking is already " + booking.getStatus().name().toLowerCase());
        }
        if (personId != null && !booking.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The booking has already started and can no longer be cancelled");
        }
        booking.setStatus(RentalBookingStatus.CANCELLED);
        bookings.save(booking);
        auditService.log("CANCELLED", "RENTAL_BOOKING", bookingId,
                "Rental booking cancelled: " + booking.getObject().getName() + " " + booking.getStartAt());
    }

    /**
     * REN-8 and REN-13: the borrower, or an administrator on their behalf, hands the object back and
     * may note a fault. Unlike cancelling, this is allowed after the start time — that is the point of
     * it, and the deliberate exception to REN-7.
     */
    public RentalBookingResponse markReturned(Long bookingId, Long personId, RentalReturnRequest req) {
        var booking = ownedOrThrow(bookingId, personId);
        if (booking.getStatus() != RentalBookingStatus.CONFIRMED) {
            throw new ConflictException("Only a confirmed booking can be returned; this one is "
                    + booking.getStatus().name().toLowerCase());
        }
        if (booking.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("The booking has not started yet, so it cannot be returned");
        }
        booking.setStatus(RentalBookingStatus.RETURNED);
        booking.setReturnedAt(LocalDateTime.now());
        var saved = bookings.save(booking);

        var description = req == null || req.faultDescription() == null ? "" : req.faultDescription().trim();
        if (!description.isEmpty()) {
            var report = new RentalFaultReport();
            report.setBooking(saved);
            report.setDescription(description);
            faults.save(report);
            auditService.log("FAULT_REPORTED", "RENTAL_BOOKING", bookingId,
                    "Fault reported on " + saved.getObject().getName() + ": " + description);
        }
        auditService.log("RETURNED", "RENTAL_BOOKING", bookingId,
                "Rental returned: " + saved.getObject().getName());
        return RentalBookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RentalFaultReportResponse> openFaults() {
        return faults.findByResolvedAtIsNullOrderByReportedAtDesc().stream()
                .map(RentalFaultReportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalFaultReportResponse> resolvedFaults() {
        return faults.findByResolvedAtIsNotNullOrderByReportedAtDesc().stream()
                .map(RentalFaultReportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalFaultReportResponse> faultsForObject(Long objectId) {
        return faults.findByBookingObjectIdOrderByReportedAtDesc(objectId).stream()
                .map(RentalFaultReportResponse::from).toList();
    }

    public void resolveFault(Long faultId) {
        var report = faults.findById(faultId)
                .orElseThrow(() -> new ResourceNotFoundException("Fault report " + faultId + " was not found"));
        if (report.getResolvedAt() != null) {
            throw new ConflictException("The fault report is already handled");
        }
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(currentUsername());
        faults.save(report);
        auditService.log("FAULT_RESOLVED", "RENTAL_BOOKING", report.getBooking().getId(),
                "Fault report handled on " + report.getBooking().getObject().getName());
    }

    /** The acting user, resolved the same way AuditService resolves it. */
    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "system";
    }

    RentalBooking ownedOrThrow(Long bookingId, Long personId) {
        var booking = bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental booking " + bookingId + " was not found"));
        if (personId != null && !booking.getPerson().getId().equals(personId)) {
            throw new ResourceNotFoundException("Rental booking " + bookingId + " was not found");
        }
        return booking;
    }
}
