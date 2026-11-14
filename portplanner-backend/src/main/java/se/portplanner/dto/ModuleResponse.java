package se.portplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** A per-user view of a module: it reflects the signed-in user's role and access level. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleResponse(String id, String name, boolean enabled, boolean accessible, String access,
                             Map<String, Object> settings) {
}
