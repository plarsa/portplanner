package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.UserCreatedResponse;
import se.portplanner.dto.UserRequest;
import se.portplanner.dto.UserResponse;
import se.portplanner.security.Actor;
import se.portplanner.service.UserService;

import java.util.List;

/** Kontohantering. Styrs av rollreglerna i UserPolicy, inte av modulnivåer (RÄT-7). */
@RestController
@RequestMapping("/api")
@Tag(name = "Users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/roles")
    @Operation(summary = "Roller som går att tilldela (härledda från modulernas access)")
    public List<String> roles(Authentication auth) {
        return users.assignableRoles(Actor.from(auth));
    }

    @GetMapping("/users")
    @Operation(summary = "Lista konton, inklusive konfigurerad superadmin (skrivskyddad)")
    public List<UserResponse> list(@RequestParam(required = false) String role,
                                   @RequestParam(required = false) String status,
                                   Authentication auth) {
        return users.list(Actor.from(auth), role, status);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Hämta ett konto")
    public UserResponse get(@PathVariable Long id, Authentication auth) {
        return users.get(Actor.from(auth), id);
    }

    @PostMapping("/users")
    @Operation(summary = "Skapa konto. Engångslösenordet returneras en gång")
    public ResponseEntity<UserCreatedResponse> create(@Valid @RequestBody UserRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(Actor.from(auth), req));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Ersätt e-post, roller, inloggningsmetoder och personkoppling")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req, Authentication auth) {
        return users.update(Actor.from(auth), id, req);
    }

    @PostMapping("/users/{id}/disable")
    @Operation(summary = "Inaktivera konto och avsluta dess sessioner")
    public UserResponse disable(@PathVariable Long id, Authentication auth) {
        return users.disable(Actor.from(auth), id);
    }

    @PostMapping("/users/{id}/enable")
    @Operation(summary = "Återaktivera konto")
    public UserResponse enable(@PathVariable Long id, Authentication auth) {
        return users.enable(Actor.from(auth), id);
    }
}
