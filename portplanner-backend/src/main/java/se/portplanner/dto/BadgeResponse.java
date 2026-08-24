package se.portplanner.dto;

import se.portplanner.dashboard.DashboardBadge;

public record BadgeResponse(String id, String name, String description, String value) {

    public static BadgeResponse from(DashboardBadge badge) {
        return new BadgeResponse(badge.getId(), badge.getName(), badge.getDescription(), badge.getValue());
    }
}
