package se.portplanner.module;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings shared by every module under {@code portplanner.modules.<id>}.
 * Module-specific settings are bound in their own classes, such as {@link StorageProperties}.
 */
@ConfigurationProperties("portplanner")
public class ModulesProperties {

    private Map<String, ModuleConfig> modules = new HashMap<>();

    public Map<String, ModuleConfig> getModules() { return modules; }
    public void setModules(Map<String, ModuleConfig> modules) { this.modules = modules; }

    public static class ModuleConfig {
        private boolean enabled;
        /** {@code role:level} entries that act as ceilings. Null means use the module's default entries. */
        private List<RoleGrant> access;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<RoleGrant> getAccess() { return access; }
        public void setAccess(List<RoleGrant> access) { this.access = access; }
    }
}
