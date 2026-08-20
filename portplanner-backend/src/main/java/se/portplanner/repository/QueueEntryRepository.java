package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.QueueEntry;
import se.portplanner.model.QueueEntryStatus;

import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findByStatusOrderByRequestedDateAsc(QueueEntryStatus status);
}
