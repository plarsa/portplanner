package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.WinterSeason;

import java.util.List;

public interface WinterSeasonRepository extends JpaRepository<WinterSeason, Long> {
    List<WinterSeason> findAllByOrderByYearDesc();
}
