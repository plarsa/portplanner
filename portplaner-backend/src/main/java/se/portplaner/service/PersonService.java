package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.PersonRequest;
import se.portplaner.dto.PersonResponse;
import se.portplaner.exception.ResourceNotFoundException;
import se.portplaner.model.Person;
import se.portplaner.repository.PersonRepository;

import java.util.List;

@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final AuditService auditService;

    public PersonService(PersonRepository personRepository, AuditService auditService) {
        this.personRepository = personRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll(String search) {
        List<Person> persons = (search != null && !search.isBlank())
                ? personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, search)
                : personRepository.findAll();
        return persons.stream().map(PersonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PersonResponse findById(Long id) {
        return PersonResponse.from(getOrThrow(id));
    }

    public PersonResponse create(PersonRequest req) {
        var person = new Person();
        mapFields(person, req);
        var saved = personRepository.save(person);
        auditService.log("CREATED", "PERSON", saved.getId(),
                "Person skapad: " + saved.getFirstName() + " " + saved.getLastName() + " (" + saved.getEmail() + ")");
        return PersonResponse.from(saved);
    }

    public PersonResponse update(Long id, PersonRequest req) {
        var person = getOrThrow(id);
        mapFields(person, req);
        var saved = personRepository.save(person);
        auditService.log("UPDATED", "PERSON", saved.getId(),
                "Person uppdaterad: " + saved.getFirstName() + " " + saved.getLastName());
        return PersonResponse.from(saved);
    }

    public void delete(Long id) {
        var person = getOrThrow(id);
        String name = person.getFirstName() + " " + person.getLastName();
        personRepository.delete(person);
        auditService.log("DELETED", "PERSON", id, "Person borttagen: " + name);
    }

    private Person getOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person " + id + " hittades inte"));
    }

    private void mapFields(Person p, PersonRequest req) {
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setEmail(req.email());
        p.setPhone(req.phone());
    }
}
