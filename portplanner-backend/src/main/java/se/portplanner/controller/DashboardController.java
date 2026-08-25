package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.portplanner.dto.BadgeResponse;
import se.portplanner.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/badges")
    @Operation(summary = "Hämta alla tillgängliga dashboard-badges med aktuella värden")
    public List<BadgeResponse> getBadges() {
        return dashboardService.getAllBadges();
    }
}
