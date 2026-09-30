package se.portplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Personlig vy av en modul: speglar den inloggades roll och åtkomstnivå. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleResponse(String id, String name, boolean enabled, boolean accessible, String access,
                             Map<String, Object> settings) {
}
