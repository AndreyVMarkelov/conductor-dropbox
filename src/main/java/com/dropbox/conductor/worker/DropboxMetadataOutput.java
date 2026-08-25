package com.dropbox.conductor.worker;

import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import java.util.HashMap;
import java.util.Map;

public final class DropboxMetadataOutput {

    private DropboxMetadataOutput() {}

    public static Map<String, Object> from(Metadata metadata) {
        Map<String, Object> output = new HashMap<>();

        output.put("name", metadata.getName());
        putIfNotNull(output, "pathLower", metadata.getPathLower());
        putIfNotNull(output, "pathDisplay", metadata.getPathDisplay());

        switch (metadata) {
            case FileMetadata file -> {
                output.put("type", "file");
                output.put("id", file.getId());
                output.put("rev", file.getRev());
                output.put("size", file.getSize());
                putIfNotNull(output, "contentHash", file.getContentHash());
            }
            case FolderMetadata folder -> {
                output.put("type", "folder");
                output.put("id", folder.getId());
            }
            case DeletedMetadata ignored -> output.put("type", "deleted");
            default -> output.put("type", "unknown");
        }

        return output;
    }

    private static void putIfNotNull(Map<String, Object> output, String key, Object value) {
        if (value != null) {
            output.put(key, value);
        }
    }
}
