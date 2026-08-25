package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class DownloadFileWorker implements Worker {

    public static final String TASK_NAME = "dropbox_download_file";

    private final DbxClientV2 dropbox;

    public DownloadFileWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String path = (String) task.getInputData().get("path");

        if (path == null || path.isBlank()) {
            return TaskResults.invalidInput(task, "download_file", "path is required");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            FileMetadata metadata = dropbox.files().downloadBuilder(path).download(outputStream);

            Map<String, Object> output = fileOutput(metadata);
            output.put("content", Base64.getEncoder().encodeToString(outputStream.toByteArray()));

            return TaskResults.completed(task, output);
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("download_file", e));
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
