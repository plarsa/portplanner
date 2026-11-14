package se.portplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The build version of the running service, read from META-INF/build-info.properties
 * which the Spring Boot Maven plugin generates. Reported in the startup log and over
 * the API so every page can show it.
 */
@Component
public class VersionInfo {

    private static final Logger log = LoggerFactory.getLogger(VersionInfo.class);
    private static final String UNKNOWN = "unknown";

    private final String version;

    public VersionInfo(ObjectProvider<BuildProperties> buildProperties) {
        var build = buildProperties.getIfAvailable();
        this.version = build == null || build.getVersion() == null ? UNKNOWN : build.getVersion();
    }

    public String getVersion() {
        return version;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logVersion() {
        log.info("Portplanner {} started", version);
    }
}
