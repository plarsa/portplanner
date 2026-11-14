package se.portplanner.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.portplanner.model.RentalObject;

import java.util.List;
import java.util.Optional;

public interface RentalObjectRepository extends JpaRepository<RentalObject, Long> {

    List<RentalObject> findByBookableTrueOrderByNameAsc();

    /**
     * Takes a write lock on the object row. Every booking goes through this first, so two
     * simultaneous requests for the same object are serialised and the overlap check below
     * cannot be run by both of them at once.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RentalObject o where o.id = :id")
    Optional<RentalObject> findByIdForUpdate(@Param("id") Long id);
}
