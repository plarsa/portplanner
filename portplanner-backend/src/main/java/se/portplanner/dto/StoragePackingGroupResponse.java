package se.portplanner.dto;

import se.portplanner.model.StoragePackingGroup;

public record StoragePackingGroupResponse(Long id, Long yardId, String name, String retrievalNote) {
    public static StoragePackingGroupResponse from(StoragePackingGroup g) {
        return new StoragePackingGroupResponse(g.getId(), g.getYard().getId(), g.getName(), g.getRetrievalNote());
    }
}
