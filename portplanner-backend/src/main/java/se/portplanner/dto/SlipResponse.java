package se.portplanner.dto;

import se.portplanner.model.Slip;
import se.portplanner.model.SlipStatus;

import java.math.BigDecimal;

public record SlipResponse(Long id, String slipNumber,
                           BigDecimal maxLengthM, BigDecimal maxWidthM, BigDecimal maxDraftM,
                           Long dockId, String dockName, SlipStatus status,
                           String mooringType, String side, String category, String notes) {
    public static SlipResponse from(Slip s) {
        return new SlipResponse(s.getId(), s.getSlipNumber(),
                s.getMaxLengthM(), s.getMaxWidthM(), s.getMaxDraftM(),
                s.getDock().getId(), s.getDock().getName(), s.getStatus(),
                s.getMooringType(), s.getSide(), s.getCategory(), s.getNotes());
    }
}
