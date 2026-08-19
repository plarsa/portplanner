package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.TariffRequest;
import se.portplanner.dto.TariffResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.Tariff;
import se.portplanner.repository.TariffRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AuditService auditService;

    public TariffService(TariffRepository tariffRepository, AuditService auditService) {
        this.tariffRepository = tariffRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> findAll() {
        return tariffRepository.findAllByOrderByCategoryAscValidFromDesc()
                .stream().map(TariffResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> findActiveToday() {
        LocalDate today = LocalDate.now();
        return tariffRepository.findAll().stream()
                .filter(t -> !t.getValidFrom().isAfter(today)
                        && (t.getValidTo() == null || !t.getValidTo().isBefore(today)))
                .map(TariffResponse::from)
                .toList();
    }

    public TariffResponse create(TariffRequest req) {
        var tariff = new Tariff();
        mapFields(tariff, req);
        var saved = tariffRepository.save(tariff);
        auditService.log("CREATED", "TARIFF", saved.getId(),
                "Tariff skapad: " + saved.getName() + " (" + saved.getCategory() + ") " + saved.getAnnualFeeKr() + " kr/år");
        return TariffResponse.from(saved);
    }

    public TariffResponse update(Long id, TariffRequest req) {
        var tariff = getOrThrow(id);
        mapFields(tariff, req);
        var saved = tariffRepository.save(tariff);
        auditService.log("UPDATED", "TARIFF", saved.getId(),
                "Tariff uppdaterad: " + saved.getName() + " (" + saved.getCategory() + ")");
        return TariffResponse.from(saved);
    }

    public void delete(Long id) {
        var tariff = getOrThrow(id);
        tariffRepository.delete(tariff);
        auditService.log("DELETED", "TARIFF", id,
                "Tariff borttagen: " + tariff.getName() + " (" + tariff.getCategory() + ")");
    }

    private Tariff getOrThrow(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff " + id + " hittades inte"));
    }

    private void mapFields(Tariff t, TariffRequest req) {
        t.setName(req.name());
        t.setCategory(req.category());
        t.setAnnualFeeKr(req.annualFeeKr());
        t.setValidFrom(req.validFrom());
        t.setValidTo(req.validTo());
        t.setDescription(req.description());
    }
}
