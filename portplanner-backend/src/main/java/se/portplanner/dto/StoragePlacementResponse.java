package se.portplanner.dto;

import se.portplanner.model.StoragePlacement;

import java.math.BigDecimal;

public record StoragePlacementResponse(Long id, Long yardId, Long bookingId,
                                       Long boatId, String boatModel, String hullType,
                                       String personName,
                                       BigDecimal xMeters, BigDecimal yMeters, BigDecimal rotationDeg,
                                       BigDecimal widthMeters, BigDecimal lengthMeters,
                                       Long packingGroupId, String packingGroupName,
                                       Integer orderInGroup, String status) {
    public static StoragePlacementResponse from(StoragePlacement p) {
        var boat = p.getBooking().getBoat();
        var person = p.getBooking().getPerson();
        var group = p.getPackingGroup();
        return new StoragePlacementResponse(
                p.getId(), p.getYard().getId(), p.getBooking().getId(),
                boat.getId(), boat.getModel(),
                boat.getHullType() != null ? boat.getHullType().name() : null,
                person.getFirstName() + " " + person.getLastName(),
                p.getXMeters(), p.getYMeters(), p.getRotationDeg(),
                p.getWidthMeters(), p.getLengthMeters(),
                group != null ? group.getId() : null,
                group != null ? group.getName() : null,
                p.getOrderInGroup(), p.getStatus().name());
    }
}
