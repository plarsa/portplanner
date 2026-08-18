package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.BoatRequest;
import se.portplaner.dto.BoatResponse;
import se.portplaner.exception.ResourceNotFoundException;
import se.portplaner.model.Boat;
import se.portplaner.repository.BoatRepository;
import se.portplaner.repository.PersonRepository;

import java.util.List;

@Service
@Transactional
public class BoatService {

    private final BoatRepository boatRepository;
    private final PersonRepository personRepository;

    public BoatService(BoatRepository boatRepository, PersonRepository personRepository) {
        this.boatRepository = boatRepository;
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public List<BoatResponse> findAll() {
        return boatRepository.findAll().stream().map(BoatResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BoatResponse> findByOwner(Long ownerId) {
        return boatRepository.findByOwnerId(ownerId).stream().map(BoatResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BoatResponse findById(Long id) {
        return BoatResponse.from(getOrThrow(id));
    }

    public BoatResponse create(BoatRequest req) {
        var boat = new Boat();
        mapFields(boat, req);
        return BoatResponse.from(boatRepository.save(boat));
    }

    public BoatResponse update(Long id, BoatRequest req) {
        var boat = getOrThrow(id);
        mapFields(boat, req);
        return BoatResponse.from(boatRepository.save(boat));
    }

    public void delete(Long id) {
        boatRepository.delete(getOrThrow(id));
    }

    private Boat getOrThrow(Long id) {
        return boatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Båt " + id + " hittades inte"));
    }

    private void mapFields(Boat boat, BoatRequest req) {
        var owner = personRepository.findById(req.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Person " + req.ownerId() + " hittades inte"));
        boat.setName(req.name());
        boat.setRegistrationNumber(req.registrationNumber());
        boat.setLengthM(req.lengthM());
        boat.setWidthM(req.widthM());
        boat.setDraftM(req.draftM());
        boat.setOwner(owner);
    }
}
