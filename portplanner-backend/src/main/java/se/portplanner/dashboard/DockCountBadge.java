package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.repository.DockRepository;

@Component
public class DockCountBadge implements DashboardBadge {

    private final DockRepository dockRepository;

    public DockCountBadge(DockRepository dockRepository) {
        this.dockRepository = dockRepository;
    }

    @Override public String getId()          { return "dock-count"; }
    @Override public String getName()        { return "Bryggor"; }
    @Override public String getDescription() { return "Totalt antal bryggor i systemet"; }

    @Override
    public String getValue() {
        return String.valueOf(dockRepository.count());
    }
}
