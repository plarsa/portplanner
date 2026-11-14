package se.portplanner.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The settings under {@code portplanner.file-store}. */
@ConfigurationProperties("portplanner.file-store")
public class FileStoreProperties {

    /** Which implementation to use. Only {@code local} exists today. */
    private String type = "local";

    private Local local = new Local();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Local getLocal() { return local; }
    public void setLocal(Local local) { this.local = local; }

    public static class Local {
        /** The directory the bytes are written under. */
        private String path;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}
