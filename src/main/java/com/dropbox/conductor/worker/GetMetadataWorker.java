package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public final class GetMetadataWorker implements Worker {

    public static final String TASK_NAME = "dropbox_get_metadata";

    private final DbxClientV2 dropbox;

    public GetMetadataWorker(DbxClientV2 dropbox) {
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
            return TaskResults.invalidInput(task, "get_metadata", "path is required");
        }

        try {
            var metadata = dropbox.files().getMetadata(path);

            return TaskResults.completed(task, DropboxMetadataOutput.from(metadata));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("get_metadata", e));
        }
    }
}
