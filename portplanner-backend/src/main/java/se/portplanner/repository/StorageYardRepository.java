package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.StorageYard;

import java.util.List;

public interface StorageYardRepository extends JpaRepository<StorageYard, Long> {
    List<StorageYard> findBySeasonIdOrderByNameAsc(Long seasonId);
}
