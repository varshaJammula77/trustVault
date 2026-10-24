package com.trustvault;

import com.trustvault.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageSecurityTest {
    @TempDir Path temp;

    @Test
    void rejectsUnixAndWindowsPathTraversal() {
        FileStorage storage = new FileStorage(temp.resolve("private").toString());
        assertThrows(SecurityException.class, () -> storage.open("../secret.txt"));
        assertThrows(SecurityException.class, () -> storage.open("..\\..\\secret.txt"));
        assertThrows(SecurityException.class, () -> storage.delete("../../secret.txt"));
    }

    @Test
    void storesAndReadsOpaqueFileAndRejectsUnknownKey() throws Exception {
        FileStorage storage = new FileStorage(temp.resolve("private").toString());
        byte[] bytes = "private test content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var saved = storage.saveBytes(bytes, "test");
        assertEquals(bytes.length, saved.size());
        assertEquals(64, saved.sha256().length());
        try (var in = storage.open(saved.key())) {
            assertArrayEquals(bytes, in.readAllBytes());
        }
        assertThrows(java.io.FileNotFoundException.class, () -> storage.open("missing-file"));
    }
}
