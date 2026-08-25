package se.portplanner.dashboard;

import org.springframework.stereotype.Component;
import se.portplanner.repository.PersonRepository;

@Component
public class PersonCountBadge implements DashboardBadge {

    private final PersonRepository personRepository;

    public PersonCountBadge(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override public String getId()          { return "person-count"; }
    @Override public String getName()        { return "Medlemmar"; }
    @Override public String getDescription() { return "Totalt antal registrerade personer"; }

    @Override
    public String getValue() {
        return String.valueOf(personRepository.count());
    }
}
