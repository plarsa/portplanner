package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.PriceCalculationResponse;
import se.portplanner.dto.PricingRuleRequest;
import se.portplanner.dto.PricingRuleResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.PricingRule;
import se.portplanner.repository.HaulOutBookingRepository;
import se.portplanner.repository.PricingRuleRepository;
import se.portplanner.repository.WinterSeasonRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class PricingRuleService {

    private final PricingRuleRepository ruleRepo;
    private final WinterSeasonRepository seasonRepo;
    private final HaulOutBookingRepository bookingRepo;
    private final AuditService auditService;

    public PricingRuleService(PricingRuleRepository ruleRepo, WinterSeasonRepository seasonRepo,
                               HaulOutBookingRepository bookingRepo, AuditService auditService) {
        this.ruleRepo = ruleRepo;
        this.seasonRepo = seasonRepo;
        this.bookingRepo = bookingRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> findBySeason(Long seasonId) {
        return ruleRepo.findBySeasonId(seasonId).stream().map(PricingRuleResponse::from).toList();
    }

    public PricingRuleResponse create(PricingRuleRequest req) {
        var season = seasonRepo.findById(req.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Vintersäsong " + req.seasonId() + " hittades inte"));
        var rule = new PricingRule();
        rule.setSeason(season);
        mapFields(rule, req);
        var saved = ruleRepo.save(rule);
        auditService.log("CREATED", "PRICING_RULE", saved.getId(), "Prisregel skapad för säsong " + season.getName());
        return PricingRuleResponse.from(saved);
    }

    public PricingRuleResponse update(Long id, PricingRuleRequest req) {
        var rule = getOrThrow(id);
        mapFields(rule, req);
        return PricingRuleResponse.from(ruleRepo.save(rule));
    }

    public void delete(Long id) {
        ruleRepo.delete(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PriceCalculationResponse calculateForBooking(Long bookingId) {
        var booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Anmälan " + bookingId + " hittades inte"));
        var boat = booking.getBoat();
        var season = booking.getSlot().getSeason();
        var rule = ruleRepo.findFirstBySeasonId(season.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ingen prisregel för säsongen"));

        var length = boat.getLengthM();
        var width = boat.getWidthM();
        var area = length.multiply(width).setScale(2, RoundingMode.HALF_UP);
        var base = area.multiply(rule.getPricePerSqm()).setScale(2, RoundingMode.HALF_UP);

        var widthSurcharge = BigDecimal.ZERO;
        if (rule.getExtraWidthThresholdM() != null && rule.getExtraWidthSurchargePerDm() != null
                && width.compareTo(rule.getExtraWidthThresholdM()) > 0) {
            var excess = width.subtract(rule.getExtraWidthThresholdM());
            var dm = excess.divide(new BigDecimal("0.1"), 2, RoundingMode.HALF_UP);
            widthSurcharge = dm.multiply(rule.getExtraWidthSurchargePerDm()).setScale(2, RoundingMode.HALF_UP);
        }

        var lengthSurcharge = BigDecimal.ZERO;
        if (rule.getExtraLengthThresholdM() != null && rule.getExtraLengthSurchargePerDm() != null
                && length.compareTo(rule.getExtraLengthThresholdM()) > 0) {
            var excess = length.subtract(rule.getExtraLengthThresholdM());
            var dm = excess.divide(new BigDecimal("0.1"), 2, RoundingMode.HALF_UP);
            lengthSurcharge = dm.multiply(rule.getExtraLengthSurchargePerDm()).setScale(2, RoundingMode.HALF_UP);
        }

        var total = base.add(widthSurcharge).add(lengthSurcharge);
        if (rule.getMinPrice() != null && total.compareTo(rule.getMinPrice()) < 0) {
            total = rule.getMinPrice();
        }

        return new PriceCalculationResponse(bookingId, boat.getModel(), length, width,
                area, base, widthSurcharge, lengthSurcharge, total);
    }

    private PricingRule getOrThrow(Long id) {
        return ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prisregel " + id + " hittades inte"));
    }

    private void mapFields(PricingRule rule, PricingRuleRequest req) {
        rule.setPricePerSqm(req.pricePerSqm());
        rule.setExtraWidthThresholdM(req.extraWidthThresholdM());
        rule.setExtraWidthSurchargePerDm(req.extraWidthSurchargePerDm());
        rule.setExtraLengthThresholdM(req.extraLengthThresholdM());
        rule.setExtraLengthSurchargePerDm(req.extraLengthSurchargePerDm());
        rule.setMinPrice(req.minPrice());
    }
}
