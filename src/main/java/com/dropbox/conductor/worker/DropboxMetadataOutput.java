package com.dropbox.conductor.worker;

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

        if (metadata instanceof FileMetadata file) {
            output.put("type", "file");
            output.put("id", file.getId());
            output.put("rev", file.getRev());
            output.put("size", file.getSize());
            output.put("contentHash", file.getContentHash());
        } else if (metadata instanceof FolderMetadata folder) {
            output.put("type", "folder");
            output.put("id", folder.getId());
        } else {
            output.put("type", "deleted");
        }

        return output;
    }

    private static void putIfNotNull(Map<String, Object> output, String key, Object value) {
        if (value != null) {
            output.put(key, value);
        }
    }
}
