package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPersonId(Long personId);
}
