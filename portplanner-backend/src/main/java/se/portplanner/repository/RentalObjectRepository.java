package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.RentalObject;

import java.util.List;

public interface RentalObjectRepository extends JpaRepository<RentalObject, Long> {

    List<RentalObject> findByBookableTrueOrderByNameAsc();
}
