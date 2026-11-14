package se.portplanner.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.portplanner.exception.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStoreTest {

    @TempDir Path dir;

    private LocalFileStore store() {
        return new LocalFileStore(dir.toString());
    }

    @Test
    void putThenGetReturnsTheSameBytes() throws Exception {
        var key = store().put(new ByteArrayInputStream("hello".getBytes(UTF_8)), "image/png", 5);
        assertEquals("hello", new String(store().get(key).readAllBytes(), UTF_8));
    }

    @Test
    void deleteRemovesTheFile() {
        var s = store();
        var key = s.put(new ByteArrayInputStream("hello".getBytes(UTF_8)), "image/png", 5);
        s.delete(key);
        assertThrows(ResourceNotFoundException.class, () -> s.get(key));
    }

    @Test
    void getUnknownKeyThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> store().get("00000000000000000000000000000000"));
    }

    @Test
    void deletingAnUnknownKeyIsSilent() {
        store().delete("00000000000000000000000000000000");
    }

    @Test
    void nameIsLocal() {
        assertEquals("local", store().name());
    }
}
