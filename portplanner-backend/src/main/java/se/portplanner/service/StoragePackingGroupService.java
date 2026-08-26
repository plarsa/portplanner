package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.StoragePackingGroupRequest;
import se.portplanner.dto.StoragePackingGroupResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.StoragePackingGroup;
import se.portplanner.repository.StoragePackingGroupRepository;
import se.portplanner.repository.StorageYardRepository;

import java.util.List;

@Service
@Transactional
public class StoragePackingGroupService {

    private final StoragePackingGroupRepository groupRepo;
    private final StorageYardRepository yardRepo;
    private final AuditService auditService;

    public StoragePackingGroupService(StoragePackingGroupRepository groupRepo,
                                       StorageYardRepository yardRepo, AuditService auditService) {
        this.groupRepo = groupRepo;
        this.yardRepo = yardRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StoragePackingGroupResponse> findByYard(Long yardId) {
        return groupRepo.findByYardIdOrderByNameAsc(yardId).stream()
                .map(StoragePackingGroupResponse::from).toList();
    }

    public StoragePackingGroupResponse create(StoragePackingGroupRequest req) {
        var yard = yardRepo.findById(req.yardId())
                .orElseThrow(() -> new ResourceNotFoundException("Gårdsplan " + req.yardId() + " hittades inte"));
        var group = new StoragePackingGroup();
        group.setYard(yard);
        group.setName(req.name());
        group.setRetrievalNote(req.retrievalNote());
        return StoragePackingGroupResponse.from(groupRepo.save(group));
    }

    public StoragePackingGroupResponse update(Long id, StoragePackingGroupRequest req) {
        var group = getOrThrow(id);
        group.setName(req.name());
        group.setRetrievalNote(req.retrievalNote());
        return StoragePackingGroupResponse.from(groupRepo.save(group));
    }

    public void delete(Long id) {
        groupRepo.delete(getOrThrow(id));
    }

    private StoragePackingGroup getOrThrow(Long id) {
        return groupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Packningsgrupp " + id + " hittades inte"));
    }
}
