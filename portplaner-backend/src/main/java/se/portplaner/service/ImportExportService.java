package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.*;
import se.portplaner.model.Dock;
import se.portplaner.model.Slip;
import se.portplaner.model.SlipStatus;
import se.portplaner.repository.DockRepository;
import se.portplaner.repository.SlipRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ImportExportService {

    private final DockRepository dockRepository;
    private final SlipRepository slipRepository;

    public ImportExportService(DockRepository dockRepository, SlipRepository slipRepository) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
    }

    // ── Export ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DockImportDto> exportDocks() {
        return dockRepository.findAll().stream()
                .map(dock -> {
                    var slips = slipRepository.findByDockId(dock.getId()).stream()
                            .map(s -> new SlipImportDto(
                                    s.getSlipNumber(),
                                    s.getMaxWidthM(),
                                    s.getMaxLengthM(),
                                    s.getMaxDraftM(),
                                    s.getMooringType(),
                                    s.getSide()))
                            .toList();
                    return new DockImportDto(dock.getName(), dock.getDescription(), slips);
                })
                .toList();
    }

    // ── Preview (dry-run) ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ImportPreview preview(List<DockImportDto> docks) {
        Map<String, Dock> existing = existingDocksByName();
        int docksNew = 0, docksExisting = 0, slipsNew = 0, slipsSkipped = 0;
        List<String> details = new ArrayList<>();

        for (DockImportDto dto : docks) {
            Dock existingDock = existing.get(dto.name().toLowerCase());
            if (existingDock == null) {
                docksNew++;
                details.add("Ny brygga: " + dto.name() +
                        (dto.slips() != null ? " (" + dto.slips().size() + " platser)" : ""));
                if (dto.slips() != null) slipsNew += dto.slips().size();
            } else {
                docksExisting++;
                Set<String> existingSlipNums = slipRepository.findByDockId(existingDock.getId())
                        .stream().map(Slip::getSlipNumber).collect(Collectors.toSet());
                if (dto.slips() != null) {
                    long toAdd = dto.slips().stream()
                            .filter(s -> !existingSlipNums.contains(s.slipNumber())).count();
                    long toSkip = dto.slips().size() - toAdd;
                    slipsNew += toAdd;
                    slipsSkipped += toSkip;
                    details.add("Befintlig brygga: " + dto.name() +
                            " – " + toAdd + " nya platser, " + toSkip + " hoppar över");
                }
            }
        }
        return new ImportPreview(docksNew, docksExisting, slipsNew, slipsSkipped, details);
    }

    // ── Import ─────────────────────────────────────────────────────────────

    public ImportResult importDocks(List<DockImportDto> docks) {
        Map<String, Dock> existing = existingDocksByName();
        int docksCreated = 0, slipsCreated = 0, slipsSkipped = 0;
        List<String> warnings = new ArrayList<>();

        for (DockImportDto dto : docks) {
            if (dto.name() == null || dto.name().isBlank()) {
                warnings.add("En post saknar bryggonamn, hoppar över");
                continue;
            }

            Dock dock = existing.get(dto.name().toLowerCase());
            if (dock == null) {
                dock = new Dock();
                dock.setName(dto.name());
                dock.setDescription(dto.description());
                dock = dockRepository.save(dock);
                existing.put(dto.name().toLowerCase(), dock);
                docksCreated++;
            } else if (dto.description() != null && !dto.description().isBlank()) {
                dock.setDescription(dto.description());
                dockRepository.save(dock);
            }

            if (dto.slips() == null) continue;

            Set<String> existingSlipNums = slipRepository.findByDockId(dock.getId())
                    .stream().map(Slip::getSlipNumber).collect(Collectors.toSet());

            for (SlipImportDto slipDto : dto.slips()) {
                if (slipDto.slipNumber() == null || slipDto.slipNumber().isBlank()) {
                    warnings.add("Brygga " + dto.name() + ": plats utan nummer, hoppar över");
                    slipsSkipped++;
                    continue;
                }
                if (existingSlipNums.contains(slipDto.slipNumber())) {
                    slipsSkipped++;
                    continue;
                }
                if (slipDto.maxWidthM() == null) {
                    warnings.add("Brygga " + dto.name() + " plats " + slipDto.slipNumber() +
                            ": bredd saknas, hoppar över");
                    slipsSkipped++;
                    continue;
                }

                var slip = new Slip();
                slip.setDock(dock);
                slip.setSlipNumber(slipDto.slipNumber());
                slip.setMaxWidthM(slipDto.maxWidthM());
                slip.setMaxLengthM(slipDto.maxLengthM() != null ? slipDto.maxLengthM() : new java.math.BigDecimal("99.0"));
                slip.setMaxDraftM(slipDto.maxDraftM());
                slip.setMooringType(slipDto.mooringType());
                slip.setSide(slipDto.side());
                slip.setStatus(SlipStatus.AVAILABLE);
                slipRepository.save(slip);
                slipsCreated++;
                existingSlipNums.add(slipDto.slipNumber());
            }
        }
        return new ImportResult(docksCreated, slipsCreated, slipsSkipped, warnings);
    }

    private Map<String, Dock> existingDocksByName() {
        return dockRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getName().toLowerCase(), Function.identity()));
    }
}
