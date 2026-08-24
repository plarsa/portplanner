package se.portplanner.dashboard;

import org.springframework.stereotype.Component;

@Component
public class TotalRevenueBadge implements DashboardBadge {

    private final RevenueCalculator revenueCalculator;

    public TotalRevenueBadge(RevenueCalculator revenueCalculator) {
        this.revenueCalculator = revenueCalculator;
    }

    @Override public String getId()          { return "total-revenue"; }
    @Override public String getName()        { return "Maximal intäkt"; }
    @Override public String getDescription() { return "Teoretisk maximal intäkt vid fullt uthyrd anläggning"; }

    @Override
    public String getValue() {
        return RevenueCalculator.formatKr(revenueCalculator.calculate().total());
    }
}
