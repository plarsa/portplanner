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

/** Account management. Governed by the role rules in UserPolicy, not by module levels (RAT-7). */
@RestController
@RequestMapping("/api")
@Tag(name = "Users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/roles")
    @Operation(summary = "Roles that can be assigned (derived from the modules' access lists)")
    public List<String> roles(Authentication auth) {
        return users.assignableRoles(Actor.from(auth));
    }

    @GetMapping("/users")
    @Operation(summary = "List accounts, including the configured superadmin (read-only)")
    public List<UserResponse> list(@RequestParam(required = false) String role,
                                   @RequestParam(required = false) String status,
                                   Authentication auth) {
        return users.list(Actor.from(auth), role, status);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get an account")
    public UserResponse get(@PathVariable Long id, Authentication auth) {
        return users.get(Actor.from(auth), id);
    }

    @PostMapping("/users")
    @Operation(summary = "Create an account. The one-time password is returned once")
    public ResponseEntity<UserCreatedResponse> create(@Valid @RequestBody UserRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(Actor.from(auth), req));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Replace email, roles, login methods and person link")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req, Authentication auth) {
        return users.update(Actor.from(auth), id, req);
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "Issue a new one-time password. It is returned once")
    public UserCreatedResponse resetPassword(@PathVariable Long id, Authentication auth) {
        return users.resetPassword(Actor.from(auth), id);
    }

    @PostMapping("/users/{id}/disable")
    @Operation(summary = "Disable an account and end its sessions")
    public UserResponse disable(@PathVariable Long id, Authentication auth) {
        return users.disable(Actor.from(auth), id);
    }

    @PostMapping("/users/{id}/enable")
    @Operation(summary = "Re-enable an account")
    public UserResponse enable(@PathVariable Long id, Authentication auth) {
        return users.enable(Actor.from(auth), id);
    }
}
