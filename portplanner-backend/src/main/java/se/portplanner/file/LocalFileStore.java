package se.portplanner.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.portplanner.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Keeps uploaded bytes under one directory. A key is a dash-free UUID, and the file is written
 * two levels deep (first two characters, then the next two) so that no single directory ends up
 * holding every upload.
 */
public class LocalFileStore implements FileStore {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStore.class);

    private final Path root;

    public LocalFileStore(String root) {
        this.root = Path.of(root);
    }

    @Override
    public String put(InputStream data, String contentType, long size) {
        var key = UUID.randomUUID().toString().replace("-", "");
        var file = resolve(key);
        try {
            Files.createDirectories(file.getParent());
            Files.copy(data, file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file " + key, e);
        }
        log.debug("Stored file {} ({} bytes, {})", key, size, contentType);
        return key;
    }

    @Override
    public InputStream get(String key) {
        var file = resolve(key);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Stored file " + key + " was not found");
        }
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file " + key, e);
        }
    }

    @Override
    public String name() {
        return "local";
    }

    private Path resolve(String key) {
        if (key == null || key.length() < 4) {
            throw new ResourceNotFoundException("Stored file " + key + " was not found");
        }
        return root.resolve(key.substring(0, 2)).resolve(key.substring(2, 4)).resolve(key);
    }
}
