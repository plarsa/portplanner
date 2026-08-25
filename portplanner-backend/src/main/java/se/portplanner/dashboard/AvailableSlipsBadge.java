package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.model.SlipStatus;
import se.portplanner.repository.SlipRepository;

@Component
public class AvailableSlipsBadge implements DashboardBadge {

    private final SlipRepository slipRepository;

    public AvailableSlipsBadge(SlipRepository slipRepository) {
        this.slipRepository = slipRepository;
    }

    @Override public String getId()          { return "available-slips"; }
    @Override public String getName()        { return "Lediga platser"; }
    @Override public String getDescription() { return "Antal båtplatser med status Ledig"; }

    @Override
    public String getValue() {
        long total = slipRepository.count();
        long available = slipRepository.findByStatus(SlipStatus.AVAILABLE).size();
        return available + " av " + total;
    }
}
