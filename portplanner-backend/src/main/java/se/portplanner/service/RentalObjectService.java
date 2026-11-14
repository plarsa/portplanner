package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.portplanner.dto.RentalObjectRequest;
import se.portplanner.dto.RentalObjectResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.file.FileStore;
import se.portplanner.file.StoredFile;
import se.portplanner.file.StoredFileContent;
import se.portplanner.file.StoredFileRepository;
import se.portplanner.model.RentalObject;
import se.portplanner.exception.ConflictException;
import se.portplanner.repository.RentalBookingRepository;
import se.portplanner.repository.RentalObjectRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RentalObjectService {

    /** REN-2: only real image types are accepted, so the image endpoint can be served as-is. */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final RentalObjectRepository repo;
    private final AuditService auditService;
    private final FileStore fileStore;
    private final StoredFileRepository storedFiles;
    private final RentalBookingRepository bookings;

    public RentalObjectService(RentalObjectRepository repo, AuditService auditService,
                               FileStore fileStore, StoredFileRepository storedFiles,
                               RentalBookingRepository bookings) {
        this.repo = repo;
        this.auditService = auditService;
        this.fileStore = fileStore;
        this.storedFiles = storedFiles;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<RentalObjectResponse> findAll() {
        return repo.findAll().stream().map(RentalObjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalObjectResponse> findBookable() {
        return repo.findByBookableTrueOrderByNameAsc().stream().map(RentalObjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RentalObjectResponse findById(Long id) {
        return RentalObjectResponse.from(getOrThrow(id));
    }

    public RentalObjectResponse create(RentalObjectRequest req) {
        var o = new RentalObject();
        mapFields(o, req);
        var saved = repo.save(o);
        auditService.log("CREATED", "RENTAL_OBJECT", saved.getId(), "Rental object created: " + saved.getName());
        return RentalObjectResponse.from(saved);
    }

    public RentalObjectResponse update(Long id, RentalObjectRequest req) {
        var o = getOrThrow(id);
        mapFields(o, req);
        var saved = repo.save(o);
        auditService.log("UPDATED", "RENTAL_OBJECT", saved.getId(), "Rental object updated: " + saved.getName());
        return RentalObjectResponse.from(saved);
    }

    /** Bookings are history, so an object that has any is kept rather than deleted. */
    public void delete(Long id) {
        var o = getOrThrow(id);
        if (bookings.existsByObjectId(id)) {
            throw new ConflictException(
                    "The object has bookings and cannot be deleted. Set it to not bookable instead");
        }
        var image = o.getImage();
        if (image != null) {
            o.setImage(null);
            repo.save(o);
            discard(image);
        }
        repo.delete(o);
        auditService.log("DELETED", "RENTAL_OBJECT", id, "Rental object deleted: " + o.getName());
    }

    /** Stores a new image and discards the one it replaces, so no orphan is left in the store. */
    public void uploadImage(Long objectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The image file is empty");
        }
        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported image type: " + contentType + ". Allowed types are JPEG, PNG and WebP");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("The image is larger than the 5 MB limit");
        }

        var object = getOrThrow(objectId);
        var previous = object.getImage();

        String key;
        try (var data = file.getInputStream()) {
            key = fileStore.put(data, contentType, file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded image", e);
        }

        var stored = new StoredFile();
        stored.setStore(fileStore.name());
        stored.setStorageKey(key);
        stored.setOriginalFilename(file.getOriginalFilename());
        stored.setContentType(contentType);
        stored.setSizeBytes(file.getSize());
        object.setImage(storedFiles.save(stored));
        repo.save(object);

        if (previous != null) {
            discard(previous);
        }
        auditService.log("IMAGE_UPLOADED", "RENTAL_OBJECT", objectId,
                "Image set on rental object: " + object.getName());
    }

    @Transactional(readOnly = true)
    public StoredFileContent readImage(Long objectId) {
        var image = getOrThrow(objectId).getImage();
        if (image == null) {
            throw new ResourceNotFoundException("Rental object " + objectId + " has no image");
        }
        return new StoredFileContent(image.getContentType(), image.getSizeBytes(),
                fileStore.get(image.getStorageKey()));
    }

    public void deleteImage(Long objectId) {
        var object = getOrThrow(objectId);
        var image = object.getImage();
        if (image == null) {
            throw new ResourceNotFoundException("Rental object " + objectId + " has no image");
        }
        object.setImage(null);
        repo.save(object);
        discard(image);
        auditService.log("IMAGE_DELETED", "RENTAL_OBJECT", objectId,
                "Image removed from rental object: " + object.getName());
    }

    /** Removes both the bytes and the row that pointed at them. */
    void discard(StoredFile file) {
        fileStore.delete(file.getStorageKey());
        storedFiles.delete(file);
    }

    RentalObject getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental object " + id + " was not found"));
    }

    private void mapFields(RentalObject o, RentalObjectRequest req) {
        o.setName(req.name());
        o.setDescription(req.description());
        o.setBookable(req.bookable());
        o.setMaxDurationHours(req.maxDurationHours());
        o.setRequiresApproval(req.requiresApproval());
        o.setMaxDaysAhead(req.maxDaysAhead());
    }
}
