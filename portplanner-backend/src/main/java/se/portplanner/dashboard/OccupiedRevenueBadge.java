package se.portplanner.dashboard;

import org.springframework.stereotype.Component;

@Component
public class OccupiedRevenueBadge implements DashboardBadge {

    private final RevenueCalculator revenueCalculator;

    public OccupiedRevenueBadge(RevenueCalculator revenueCalculator) {
        this.revenueCalculator = revenueCalculator;
    }

    @Override public String getId()          { return "occupied-revenue"; }
    @Override public String getName()        { return "Intäkt tilldelade platser"; }
    @Override public String getDescription() { return "Årlig intäkt från aktuellt uthyrda platser"; }

    @Override
    public String getValue() {
        return RevenueCalculator.formatKr(revenueCalculator.calculate().occupied());
    }
}
