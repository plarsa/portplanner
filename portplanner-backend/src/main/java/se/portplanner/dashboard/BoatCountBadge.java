package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.repository.BoatRepository;

@Component
public class BoatCountBadge implements DashboardBadge {

    private final BoatRepository boatRepository;

    public BoatCountBadge(BoatRepository boatRepository) {
        this.boatRepository = boatRepository;
    }

    @Override public String getId()          { return "boat-count"; }
    @Override public String getName()        { return "Båtar"; }
    @Override public String getDescription() { return "Totalt antal registrerade båtar"; }

    @Override
    public String getValue() {
        return String.valueOf(boatRepository.count());
    }
}
