package se.portplanner.module;

import org.springframework.boot.context.properties.ConfigurationProperties;
import se.portplanner.model.Role;

import java.util.HashMap;
import java.util.Map;

/**
 * Gemensamma inställningar för alla moduler under {@code portplanner.modules.<id>}.
 * Modulspecifika inställningar binds i egna klasser (t.ex. {@link StorageProperties}).
 */
@ConfigurationProperties("portplanner")
public class ModulesProperties {

    private Map<String, ModuleConfig> modules = new HashMap<>();

    public Map<String, ModuleConfig> getModules() { return modules; }
    public void setModules(Map<String, ModuleConfig> modules) { this.modules = modules; }

    public static class ModuleConfig {
        private boolean enabled;
        /** Null = använd modulens standardnivåer. */
        private Map<Role, AccessLevel> access;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<Role, AccessLevel> getAccess() { return access; }
        public void setAccess(Map<Role, AccessLevel> access) { this.access = access; }
    }
}
