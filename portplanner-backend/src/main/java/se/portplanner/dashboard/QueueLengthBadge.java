package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.model.QueueEntryStatus;
import se.portplanner.repository.QueueEntryRepository;

import java.util.List;

@Component
public class QueueLengthBadge implements DashboardBadge {

    private final QueueEntryRepository queueRepository;

    public QueueLengthBadge(QueueEntryRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @Override public String getId()          { return "queue-length"; }
    @Override public String getName()        { return "Kölängd"; }
    @Override public String getDescription() { return "Antal personer som väntar på en båtplats"; }

    @Override
    public String getValue() {
        return String.valueOf(
            queueRepository.findByStatusInOrderByRequestedDateAsc(
                List.of(QueueEntryStatus.WAITING, QueueEntryStatus.OFFERED)
            ).size()
        );
    }
}
