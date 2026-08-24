package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dashboard.DashboardBadge;
import se.portplanner.dto.BadgeResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final List<DashboardBadge> badges;

    public DashboardService(List<DashboardBadge> badges) {
        this.badges = badges;
    }

    public List<BadgeResponse> getAllBadges() {
        return badges.stream().map(BadgeResponse::from).toList();
    }
}
