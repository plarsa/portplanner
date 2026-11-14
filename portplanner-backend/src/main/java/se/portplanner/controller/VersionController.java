package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.portplanner.config.VersionInfo;
import se.portplanner.dto.VersionResponse;

@RestController
@RequestMapping("/api/version")
@Tag(name = "Version")
public class VersionController {

    private final VersionInfo versionInfo;

    public VersionController(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    @GetMapping
    @Operation(summary = "Build version of the running service")
    public VersionResponse get() {
        return new VersionResponse(versionInfo.getVersion());
    }
}
