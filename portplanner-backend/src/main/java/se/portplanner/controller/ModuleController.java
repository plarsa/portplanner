package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import se.portplanner.dto.ModuleResponse;
import se.portplanner.module.ModuleService;
import se.portplanner.module.PortModule;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/modules")
@Tag(name = "Modules")
public class ModuleController {

    private final ModuleService modules;

    public ModuleController(ModuleService modules) {
        this.modules = modules;
    }

    @GetMapping
    @Operation(summary = "Alla kända moduler med den inloggades åtkomstnivå")
    public Map<String, List<ModuleResponse>> list(Authentication auth) {
        return Map.of("modules", modules.modulesFor(auth));
    }

    @GetMapping("/{id}")
    @Operation(summary = "En modul med den inloggades åtkomstnivå")
    public ModuleResponse get(@PathVariable String id, Authentication auth) {
        if (PortModule.find(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Okänd modul: " + id);
        }
        return modules.moduleFor(id, auth);
    }
}
