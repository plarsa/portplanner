package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.PersonRequest;
import se.portplanner.dto.PersonResponse;
import se.portplanner.service.PersonService;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@Tag(name = "Persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    @Operation(summary = "List all persons (with optional search)")
    public List<PersonResponse> findAll(@RequestParam(required = false) String search) {
        return personService.findAll(search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a person by id")
    public PersonResponse findById(@PathVariable Long id) {
        return personService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a person")
    public PersonResponse create(@Valid @RequestBody PersonRequest req) {
        return personService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a person")
    public PersonResponse update(@PathVariable Long id, @Valid @RequestBody PersonRequest req) {
        return personService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a person")
    public void delete(@PathVariable Long id) {
        personService.delete(id);
    }
}
