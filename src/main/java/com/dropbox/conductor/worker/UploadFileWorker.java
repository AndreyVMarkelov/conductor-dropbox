package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class UploadFileWorker implements Worker {

    public static final String TASK_NAME = "dropbox_upload_file";

    private final DbxClientV2 dropbox;

    public UploadFileWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String path = (String) task.getInputData().get("path");
        String content = (String) task.getInputData().get("content");
        String mode = (String) task.getInputData().getOrDefault("mode", "ADD");
        boolean autorename = Boolean.TRUE.equals(task.getInputData().get("autorename"));

        if (path == null || path.isBlank()) {
            return TaskResults.invalidInput(task, "upload_file", "path is required");
        }

        if (content == null || content.isBlank()) {
            return TaskResults.invalidInput(task, "upload_file", "content is required");
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(content);

            WriteMode writeMode =
                    switch (mode) {
                        case "ADD" -> WriteMode.ADD;
                        case "OVERWRITE" -> WriteMode.OVERWRITE;
                        default -> throw new IllegalArgumentException("Unsupported upload mode: " + mode);
                    };

            FileMetadata metadata;

            try (var input = new ByteArrayInputStream(bytes)) {
                metadata = dropbox.files()
                        .uploadBuilder(path)
                        .withMode(writeMode)
                        .withAutorename(autorename)
                        .uploadAndFinish(input);
            }

            return TaskResults.completed(task, fileOutput(metadata));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("upload_file", e));
        }
    }

    private static Map<String, Object> fileOutput(FileMetadata metadata) {
        Map<String, Object> output = new HashMap<>();

        output.put("id", metadata.getId());
        output.put("name", metadata.getName());
        output.put("rev", metadata.getRev());
        output.put("size", metadata.getSize());

        if (metadata.getContentHash() != null) {
            output.put("contentHash", metadata.getContentHash());
        }

        if (metadata.getPathLower() != null) {
            output.put("pathLower", metadata.getPathLower());
        }

        if (metadata.getPathDisplay() != null) {
            output.put("pathDisplay", metadata.getPathDisplay());
        }

        return output;
    }
}
