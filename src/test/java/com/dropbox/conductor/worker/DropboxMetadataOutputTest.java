package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DropboxMetadataOutputTest {

    @Test
    void mapsFileMetadata() {
        Date timestamp = new Date();
        FileMetadata metadata = FileMetadata.newBuilder("hello.txt", "id:file-1", timestamp, timestamp, "abcdef123", 14)
                .withPathLower("/folder/hello.txt")
                .withPathDisplay("/Folder/hello.txt")
                .withContentHash("0123456789abcdef0123456789abcdef" + "0123456789abcdef0123456789abcdef")
                .build();

        Map<String, Object> output = DropboxMetadataOutput.from(metadata);

        assertEquals("file", output.get("type"));
        assertEquals("hello.txt", output.get("name"));
        assertEquals("id:file-1", output.get("id"));
        assertEquals("abcdef123", output.get("rev"));
        assertEquals(14L, output.get("size"));
        assertEquals(
                "0123456789abcdef0123456789abcdef" + "0123456789abcdef0123456789abcdef", output.get("contentHash"));
        assertEquals("/folder/hello.txt", output.get("pathLower"));
        assertEquals("/Folder/hello.txt", output.get("pathDisplay"));
    }

    @Test
    void mapsFolderMetadata() {
        FolderMetadata metadata = FolderMetadata.newBuilder("Processed", "id:folder-1")
                .withPathLower("/processed")
                .withPathDisplay("/Processed")
                .build();

        Map<String, Object> output = DropboxMetadataOutput.from(metadata);

        assertEquals("folder", output.get("type"));
        assertEquals("Processed", output.get("name"));
        assertEquals("id:folder-1", output.get("id"));
        assertEquals("/processed", output.get("pathLower"));
        assertEquals("/Processed", output.get("pathDisplay"));

        assertFalse(output.containsKey("rev"));
        assertFalse(output.containsKey("size"));
        assertFalse(output.containsKey("contentHash"));
    }

    @Test
    void skipsNullPaths() {
        FolderMetadata metadata =
                FolderMetadata.newBuilder("Root", "id:folder-root").build();

        Map<String, Object> output = DropboxMetadataOutput.from(metadata);

        assertEquals("folder", output.get("type"));
        assertEquals("Root", output.get("name"));
        assertFalse(output.containsKey("pathLower"));
        assertFalse(output.containsKey("pathDisplay"));
    }
}
