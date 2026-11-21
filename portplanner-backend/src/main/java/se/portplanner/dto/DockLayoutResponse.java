package se.portplanner.dto;

import se.portplanner.model.Dock;

public record DockLayoutResponse(Long dockId, String layoutJson) {
    public static DockLayoutResponse from(Dock d) {
        return new DockLayoutResponse(d.getId(), d.getLayoutJson());
    }
}
