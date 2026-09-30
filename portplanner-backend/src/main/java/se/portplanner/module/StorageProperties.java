package se.portplanner.module;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("portplanner.modules.storage")
public class StorageProperties {

    @Pattern(regexp = "(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])", message = "ska vara MM-DD")
    private String defaultSeasonStart = "09-15";

    @Min(1)
    private int maxBoatsPerPackingGroup = 6;

    public String getDefaultSeasonStart() { return defaultSeasonStart; }
    public void setDefaultSeasonStart(String v) { this.defaultSeasonStart = v; }
    public int getMaxBoatsPerPackingGroup() { return maxBoatsPerPackingGroup; }
    public void setMaxBoatsPerPackingGroup(int v) { this.maxBoatsPerPackingGroup = v; }
}
