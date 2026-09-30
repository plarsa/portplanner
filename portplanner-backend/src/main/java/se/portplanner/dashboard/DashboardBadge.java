package se.portplanner.dashboard;

public interface DashboardBadge {
    String getId();
    String getName();
    String getDescription();
    String getValue();

    /** Id för modulen badgen tillhör; badgen döljs när modulen är avstängd eller otillgänglig. */
    default String module() { return "base"; }
}
