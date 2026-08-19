package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.ImportResult;
import se.portplaner.model.Dock;
import se.portplaner.model.Slip;
import se.portplaner.model.SlipStatus;
import se.portplaner.repository.DockRepository;
import se.portplaner.repository.SlipRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ImportService {

    private final DockRepository dockRepository;
    private final SlipRepository slipRepository;

    public ImportService(DockRepository dockRepository, SlipRepository slipRepository) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
    }

    public ImportResult importSlipsTsv(String tsvData) {
        int docksCreated = 0;
        int slipsCreated = 0;
        int slipsSkipped = 0;
        List<String> warnings = new ArrayList<>();
        Map<String, Dock> dockCache = new HashMap<>();

        String[] lines = tsvData.split("\\r?\\n");
        int startLine = 0;

        // Skip header row
        if (lines.length > 0 && lines[0].toLowerCase().contains("brygga")) {
            startLine = 1;
        }

        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;

            String[] cols = line.split("\t", -1);
            if (cols.length < 3) {
                warnings.add("Rad " + (i + 1) + ": för få kolumner, hoppar över");
                slipsSkipped++;
                continue;
            }

            String dockName  = cols[0].trim();
            String slipNum   = cols.length > 1 ? cols[1].trim() : "";
            String breddStr  = cols.length > 2 ? cols[2].trim() : "";
            String langdStr  = cols.length > 3 ? cols[3].trim() : "";
            String djupStr   = cols.length > 4 ? cols[4].trim() : "";
            String mooringType = cols.length > 5 ? cols[5].trim() : "";
            String side      = cols.length > 6 ? cols[6].trim() : "";

            if (dockName.isBlank() || slipNum.isBlank()) {
                warnings.add("Rad " + (i + 1) + ": brygga eller platsnummer saknas, hoppar över");
                slipsSkipped++;
                continue;
            }

            BigDecimal bredd = parseDecimal(breddStr);
            if (bredd == null) {
                warnings.add("Rad " + (i + 1) + " (" + dockName + " " + slipNum + "): ogiltigt breddvärde '" + breddStr + "', hoppar över");
                slipsSkipped++;
                continue;
            }

            // Längd är ofta tom – sätt 99.0 som "ej angiven max"
            BigDecimal langd = parseDecimal(langdStr);
            if (langd == null && !langdStr.isBlank()) {
                warnings.add("Rad " + (i + 1) + " (" + dockName + " " + slipNum + "): ogiltigt längdvärde '" + langdStr + "', sätter 99.0");
            }
            if (langd == null) langd = new BigDecimal("99.0");

            BigDecimal djup = parseDjup(djupStr);

            // Hämta eller skapa brygga
            Dock dock = dockCache.computeIfAbsent(dockName.toLowerCase(), key -> {
                return dockRepository.findAll().stream()
                        .filter(d -> d.getName().equalsIgnoreCase(dockName))
                        .findFirst()
                        .orElseGet(() -> {
                            var d = new Dock();
                            d.setName(dockName);
                            return dockRepository.save(d);
                        });
            });

            if (!dockCache.containsValue(dock) && dock.getId() != null) {
                docksCreated++;
            }

            // Kolla om platsen redan finns
            final String slipNumFinal = slipNum;
            final Dock dockFinal = dock;
            boolean exists = slipRepository.findByDockId(dock.getId()).stream()
                    .anyMatch(s -> s.getSlipNumber().equals(slipNumFinal));

            if (exists) {
                warnings.add("Plats " + dockName + " " + slipNum + " finns redan, hoppar över");
                slipsSkipped++;
                continue;
            }

            var slip = new Slip();
            slip.setDock(dockFinal);
            slip.setSlipNumber(slipNum);
            slip.setMaxWidthM(bredd);
            slip.setMaxLengthM(langd);
            slip.setMaxDraftM(djup);
            slip.setMooringType(mooringType.isBlank() ? null : mooringType);
            slip.setSide(side.isBlank() ? null : side);
            slip.setStatus(SlipStatus.AVAILABLE);
            slipRepository.save(slip);
            slipsCreated++;
        }

        // Räkna faktiskt skapade bryggor
        long newDocks = dockCache.values().stream()
                .filter(d -> d.getId() != null)
                .count();

        return new ImportResult((int) newDocks, slipsCreated, slipsSkipped, warnings);
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDjup(String s) {
        if (s == null || s.isBlank()) return null;
        // "0.5-1" → ta högre värdet
        if (s.contains("-")) {
            String[] parts = s.split("-");
            BigDecimal val = parseDecimal(parts[parts.length - 1]);
            if (val != null) return val;
        }
        // "ca 1.7" → ta ut siffror
        String cleaned = s.replaceAll("[^0-9.,]", "").replace(',', '.');
        if (cleaned.isBlank()) return null;
        return parseDecimal(cleaned);
    }
}
