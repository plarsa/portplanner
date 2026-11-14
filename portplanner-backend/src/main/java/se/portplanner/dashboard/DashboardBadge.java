package se.portplanner.dashboard;

public interface DashboardBadge {
    String getId();
    String getName();
    String getDescription();
    String getValue();

    /** The id of the module the badge belongs to; the badge is hidden when that module is off or out of reach. */
    default String module() { return "base"; }
}
