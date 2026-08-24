package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.model.SlipStatus;
import se.portplanner.repository.SlipRepository;

@Component
public class OccupiedSlipsBadge implements DashboardBadge {

    private final SlipRepository slipRepository;

    public OccupiedSlipsBadge(SlipRepository slipRepository) {
        this.slipRepository = slipRepository;
    }

    @Override public String getId()          { return "occupied-slips"; }
    @Override public String getName()        { return "Tilldelade platser"; }
    @Override public String getDescription() { return "Antal båtplatser som är uthyrda"; }

    @Override
    public String getValue() {
        return String.valueOf(slipRepository.findByStatus(SlipStatus.OCCUPIED).size());
    }
}
