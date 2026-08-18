package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.QueueEntry;
import se.portplaner.model.QueueEntryStatus;

import java.util.List;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    List<QueueEntry> findByStatusOrderByRequestedDateAsc(QueueEntryStatus status);
}
