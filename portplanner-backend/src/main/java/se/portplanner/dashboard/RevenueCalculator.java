package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.model.SlipStatus;
import se.portplanner.repository.SlipRepository;
import se.portplanner.repository.TariffRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class RevenueCalculator {

    private final SlipRepository slipRepository;
    private final TariffRepository tariffRepository;

    public RevenueCalculator(SlipRepository slipRepository, TariffRepository tariffRepository) {
        this.slipRepository = slipRepository;
        this.tariffRepository = tariffRepository;
    }

    public record Revenue(BigDecimal occupied, BigDecimal total) {}

    public Revenue calculate() {
        var today = LocalDate.now();
        var allTariffs = tariffRepository.findAll();

        Map<String, BigDecimal> feeByCategory = new HashMap<>();
        for (var t : allTariffs) {
            if (t.getValidFrom() != null && !t.getValidFrom().isAfter(today)
                    && (t.getValidTo() == null || !t.getValidTo().isBefore(today))) {
                feeByCategory.merge(t.getCategory(), t.getAnnualFeeKr(), BigDecimal::add);
            }
        }

        BigDecimal occupied = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (var slip : slipRepository.findAll()) {
            BigDecimal fee = slip.getCategory() != null
                    ? feeByCategory.getOrDefault(slip.getCategory(), BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            total = total.add(fee);
            if (slip.getStatus() == SlipStatus.OCCUPIED) {
                occupied = occupied.add(fee);
            }
        }
        return new Revenue(occupied, total);
    }

    public static String formatKr(BigDecimal value) {
        return NumberFormat.getIntegerInstance(new Locale("sv", "SE")).format(value) + " kr";
    }
}
