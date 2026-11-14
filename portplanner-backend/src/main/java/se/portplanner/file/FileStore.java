package se.portplanner.file;

import java.io.InputStream;

/**
 * Where uploaded bytes are kept (REN-11). An installation picks the implementation, so no
 * caller may assume a filesystem: a key is opaque and only this store can resolve it.
 *
 * <p>{@code contentType} and {@code size} are passed to {@code put} because remote stores
 * need them when the object is created; a local implementation may ignore them.
 */
public interface FileStore {

    /** Stores the bytes and returns the key that reads them back. */
    String put(InputStream data, String contentType, long size);

    /** Opens the stored bytes. Throws ResourceNotFoundException when the key is unknown. */
    InputStream get(String key);

    /** Removes the bytes. A key that is not stored is not an error. */
    void delete(String key);

    /** The name recorded on every StoredFile row, so a row can be traced to its store. */
    String name();
}
