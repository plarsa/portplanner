package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.*;
import se.portplanner.model.Dock;
import se.portplanner.model.Person;
import se.portplanner.model.Slip;
import se.portplanner.model.SlipStatus;
import se.portplanner.repository.DockRepository;
import se.portplanner.repository.PersonRepository;
import se.portplanner.repository.SlipRepository;

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
    private final PersonRepository personRepository;
    private final AuditService auditService;

    public ImportExportService(DockRepository dockRepository, SlipRepository slipRepository,
                               PersonRepository personRepository, AuditService auditService) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
        this.personRepository = personRepository;
        this.auditService = auditService;
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
        auditService.log("IMPORTED", "DOCK", null,
                "Import: " + docksCreated + " bryggor och " + slipsCreated + " platser importerade" +
                (slipsSkipped > 0 ? ", " + slipsSkipped + " hoppade över" : ""));
        return new ImportResult(docksCreated, slipsCreated, slipsSkipped, warnings);
    }

    // ── Person export ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PersonImportDto> exportPersons() {
        return personRepository.findAll().stream()
                .map(p -> new PersonImportDto(
                        p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone(),
                        p.getAddress(), p.getPostalCode(), p.getPropertyDesignation(), p.getNotes()))
                .toList();
    }

    // ── Person preview (dry-run) ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public PersonImportPreview previewPersons(List<PersonImportDto> persons) {
        Map<String, Person> existing = existingPersonsByEmail();
        int personsNew = 0, personsExisting = 0;
        List<String> details = new ArrayList<>();

        for (PersonImportDto dto : persons) {
            if (dto.email() == null || dto.email().isBlank()) continue;
            if (existing.containsKey(dto.email().toLowerCase())) {
                personsExisting++;
                details.add("Befintlig person: " + dto.firstName() + " " + dto.lastName() +
                        " (" + dto.email() + ")");
            } else {
                personsNew++;
                details.add("Ny person: " + dto.firstName() + " " + dto.lastName() +
                        " (" + dto.email() + ")");
            }
        }
        return new PersonImportPreview(personsNew, personsExisting, details);
    }

    // ── Person import ──────────────────────────────────────────────────────

    public PersonImportResult importPersons(List<PersonImportDto> persons) {
        Map<String, Person> existing = existingPersonsByEmail();
        int personsCreated = 0, personsSkipped = 0;
        List<String> warnings = new ArrayList<>();

        for (PersonImportDto dto : persons) {
            if (dto.firstName() == null || dto.firstName().isBlank() ||
                    dto.lastName() == null || dto.lastName().isBlank()) {
                warnings.add("Post saknar för- eller efternamn, hoppar över");
                personsSkipped++;
                continue;
            }
            if (dto.email() == null || dto.email().isBlank()) {
                warnings.add("Person " + dto.firstName() + " " + dto.lastName() + " saknar e-post, hoppar över");
                personsSkipped++;
                continue;
            }
            if (existing.containsKey(dto.email().toLowerCase())) {
                personsSkipped++;
                continue;
            }

            var person = new Person();
            person.setFirstName(dto.firstName());
            person.setLastName(dto.lastName());
            person.setEmail(dto.email());
            person.setPhone(dto.phone());
            person.setAddress(dto.address());
            person.setPostalCode(dto.postalCode());
            person.setPropertyDesignation(dto.propertyDesignation());
            person.setNotes(dto.notes());
            personRepository.save(person);
            existing.put(dto.email().toLowerCase(), person);
            personsCreated++;
        }

        auditService.log("IMPORTED", "PERSON", null,
                "Import: " + personsCreated + " personer importerade" +
                (personsSkipped > 0 ? ", " + personsSkipped + " hoppade över" : ""));
        return new PersonImportResult(personsCreated, personsSkipped, warnings);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Map<String, Dock> existingDocksByName() {
        return dockRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getName().toLowerCase(), Function.identity()));
    }

    private Map<String, Person> existingPersonsByEmail() {
        return personRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getEmail().toLowerCase(), Function.identity()));
    }
}
