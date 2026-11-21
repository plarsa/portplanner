package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.*;
import se.portplanner.model.Boat;
import se.portplanner.model.Dock;
import se.portplanner.model.Person;
import se.portplanner.model.Slip;
import se.portplanner.model.SlipStatus;
import se.portplanner.repository.BoatRepository;
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
    private final BoatRepository boatRepository;
    private final AuditService auditService;

    public ImportExportService(DockRepository dockRepository, SlipRepository slipRepository,
                               PersonRepository personRepository, BoatRepository boatRepository,
                               AuditService auditService) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
        this.personRepository = personRepository;
        this.boatRepository = boatRepository;
        this.auditService = auditService;
    }

    // ── The whole harbour in one file ──────────────────────────────────────

    @Transactional(readOnly = true)
    public PortplannerExport exportAll() {
        return new PortplannerExport(exportPersons(), exportBoats(), exportDocks());
    }

    @Transactional(readOnly = true)
    public ImportPreview previewAll(PortplannerExport file) {
        return new ImportPreview(
                previewPersons(orEmpty(file.persons())),
                previewBoats(orEmpty(file.boats())),
                previewDocks(orEmpty(file.docks())));
    }

    /** Persons are imported first: a boat finds its owner by email, and the owner may be in the same file. */
    public ImportResult importAll(PortplannerExport file) {
        var persons = importPersons(orEmpty(file.persons()));
        var boats = importBoats(orEmpty(file.boats()));
        var docks = importDocks(orEmpty(file.docks()));
        return new ImportResult(persons, boats, docks);
    }

    /** A file may leave a section out, which means there is nothing to import for that area. */
    private static <T> List<T> orEmpty(List<T> section) {
        return section == null ? List.of() : section;
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
    public DockImportPreview previewDocks(List<DockImportDto> docks) {
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
                    details.add("Existing dock: " + dto.name() +
                            " – " + toAdd + " new slips, " + toSkip + " skipped");
                }
            }
        }
        return new DockImportPreview(docksNew, docksExisting, slipsNew, slipsSkipped, details);
    }

    // ── Import ─────────────────────────────────────────────────────────────

    public DockImportResult importDocks(List<DockImportDto> docks) {
        Map<String, Dock> existing = existingDocksByName();
        int docksCreated = 0, slipsCreated = 0, slipsSkipped = 0;
        List<String> warnings = new ArrayList<>();

        for (DockImportDto dto : docks) {
            if (dto.name() == null || dto.name().isBlank()) {
                warnings.add("An entry has no dock name, skipping");
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
                    warnings.add("Dock " + dto.name() + ": slip without a number, skipping");
                    slipsSkipped++;
                    continue;
                }
                if (existingSlipNums.contains(slipDto.slipNumber())) {
                    slipsSkipped++;
                    continue;
                }
                if (slipDto.maxWidthM() == null) {
                    warnings.add("Dock " + dto.name() + " slip " + slipDto.slipNumber() +
                            ": width is missing, skipping");
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
                "Import: " + docksCreated + " docks and " + slipsCreated + " slips imported" +
                (slipsSkipped > 0 ? ", " + slipsSkipped + " skipped" : ""));
        return new DockImportResult(docksCreated, slipsCreated, slipsSkipped, warnings);
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
                warnings.add("An entry has no first or last name, skipping");
                personsSkipped++;
                continue;
            }
            if (dto.email() == null || dto.email().isBlank()) {
                warnings.add("Person " + dto.firstName() + " " + dto.lastName() + " has no email, skipping");
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
                "Import: " + personsCreated + " persons imported" +
                (personsSkipped > 0 ? ", " + personsSkipped + " skipped" : ""));
        return new PersonImportResult(personsCreated, personsSkipped, warnings);
    }

    // ── Boat export ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BoatImportDto> exportBoats() {
        return boatRepository.findAll().stream()
                .map(b -> new BoatImportDto(
                        b.getModel(),
                        b.getLengthM(),
                        b.getWidthM(),
                        b.getDraftM(),
                        b.getOwner().getEmail()))
                .toList();
    }

    // ── Boat preview ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BoatImportPreview previewBoats(List<BoatImportDto> boats) {
        Map<String, Person> personsByEmail = existingPersonsByEmail();
        int boatsNew = 0, boatsExisting = 0, ownersNotFound = 0;
        List<String> details = new ArrayList<>();

        for (BoatImportDto dto : boats) {
            if (dto.model() == null || dto.model().isBlank()) continue;
            Person owner = dto.ownerEmail() != null
                    ? personsByEmail.get(dto.ownerEmail().toLowerCase()) : null;
            if (owner == null) {
                ownersNotFound++;
                details.add("Owner not found: " + dto.ownerEmail() + " (boat: " + dto.model() + ")");
                continue;
            }
            if (boatRepository.existsByModelIgnoreCaseAndOwnerId(dto.model(), owner.getId())) {
                boatsExisting++;
                details.add("Existing boat: " + dto.model() + " (" + owner.getFirstName() + " " + owner.getLastName() + ")");
            } else {
                boatsNew++;
                details.add("New boat: " + dto.model() + " – owner " + owner.getFirstName() + " " + owner.getLastName());
            }
        }
        return new BoatImportPreview(boatsNew, boatsExisting, ownersNotFound, details);
    }

    // ── Boat import ────────────────────────────────────────────────────────

    public BoatImportResult importBoats(List<BoatImportDto> boats) {
        Map<String, Person> personsByEmail = existingPersonsByEmail();
        int boatsCreated = 0, boatsSkipped = 0;
        List<String> warnings = new ArrayList<>();

        for (BoatImportDto dto : boats) {
            if (dto.model() == null || dto.model().isBlank()) {
                warnings.add("An entry has no boat model, skipping");
                boatsSkipped++;
                continue;
            }
            if (dto.ownerEmail() == null || dto.ownerEmail().isBlank()) {
                warnings.add("Boat " + dto.model() + " has no owner email, skipping");
                boatsSkipped++;
                continue;
            }
            Person owner = personsByEmail.get(dto.ownerEmail().toLowerCase());
            if (owner == null) {
                warnings.add("Owner not found for email " + dto.ownerEmail() + " (boat: " + dto.model() + "), skipping");
                boatsSkipped++;
                continue;
            }
            if (boatRepository.existsByModelIgnoreCaseAndOwnerId(dto.model(), owner.getId())) {
                boatsSkipped++;
                continue;
            }
            if (dto.lengthM() == null || dto.widthM() == null) {
                warnings.add("Boat " + dto.model() + " has no length or width, skipping");
                boatsSkipped++;
                continue;
            }

            var boat = new Boat();
            boat.setModel(dto.model());
            boat.setLengthM(dto.lengthM());
            boat.setWidthM(dto.widthM());
            boat.setDraftM(dto.draftM());
            boat.setOwner(owner);
            boatRepository.save(boat);
            boatsCreated++;
        }

        auditService.log("IMPORTED", "BOAT", null,
                "Import: " + boatsCreated + " boats imported" +
                (boatsSkipped > 0 ? ", " + boatsSkipped + " skipped" : ""));
        return new BoatImportResult(boatsCreated, boatsSkipped, warnings);
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
