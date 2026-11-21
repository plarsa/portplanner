package se.portplanner.dto;

import se.portplanner.model.Dock;

public record DockResponse(Long id, String name, String description, int slipCount) {
    public static DockResponse from(Dock d) {
        return new DockResponse(d.getId(), d.getName(), d.getDescription(), d.getSlips().size());
    }
}
