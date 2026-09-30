package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.LoginRequest;
import se.portplanner.dto.LoginResponse;
import se.portplanner.security.AccountService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentisering")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountService accounts;

    public AuthController(AuthenticationManager authenticationManager, AccountService accounts) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    @Operation(summary = "Logga in och skapa session")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(auth);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        accounts.recordLogin(auth.getName());
        return ResponseEntity.ok(accounts.describe(auth));
    }

    @GetMapping("/me")
    @Operation(summary = "Kontrollera aktiv session och hämta inloggad användare")
    public ResponseEntity<LoginResponse> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(accounts.describe(auth));
    }
}
