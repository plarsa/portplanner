package se.portplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplanner.model.StoragePackingGroup;

import java.util.List;

public interface StoragePackingGroupRepository extends JpaRepository<StoragePackingGroup, Long> {
    List<StoragePackingGroup> findByYardIdOrderByNameAsc(Long yardId);
}
