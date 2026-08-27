package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.HaulOutSlot;

import java.util.List;

public interface HaulOutSlotRepository extends JpaRepository<HaulOutSlot, Long> {
    List<HaulOutSlot> findBySeasonIdOrderBySlotDateAscStartTimeAsc(Long seasonId);
}
