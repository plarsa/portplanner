package se.portplanner.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dashboard.DashboardBadge;
import se.portplanner.dto.BadgeResponse;
import se.portplanner.module.ModuleService;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final List<DashboardBadge> badges;

    private final ModuleService modules;

    public DashboardService(List<DashboardBadge> badges, ModuleService modules) {
        this.badges = badges;
        this.modules = modules;
    }

    public List<BadgeResponse> getAllBadges() {
        return badges.stream()
                .filter(b -> modules.isAccessible(b.module(), SecurityContextHolder.getContext().getAuthentication()))
                .map(BadgeResponse::from).toList();
    }
}
