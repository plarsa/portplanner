package se.portplanner.file;

import java.io.InputStream;

/** A stored file's bytes together with what is needed to serve them over HTTP. */
public record StoredFileContent(String contentType, long size, InputStream data) {
}
