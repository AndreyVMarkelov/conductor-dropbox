package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.Map;

public final class DeleteWorker implements Worker {

    public static final String TASK_NAME = "dropbox_delete";

    private final DbxClientV2 dropbox;

    public DeleteWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String path = TaskInputs.string(task, "path");

        if (path == null || path.isBlank()) {
            return TaskResults.invalidInput(task, "delete", "path is required");
        }

        try {
            Metadata metadata = dropbox.files().deleteV2(path).getMetadata();

            return TaskResults.completed(task, metadataOutput(metadata));

        } catch (DeleteErrorException deleteErrorException) {
            if (task.getRetryCount() > 0
                    && deleteErrorException.errorValue.isPathLookup()
                    && deleteErrorException.errorValue.getPathLookupValue().isNotFound()) {
                return TaskResults.completed(task, Map.of("path", path, "alreadyDeleted", true));
            }
            return TaskResults.failed(task, DropboxErrorMapper.map("delete", deleteErrorException));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("delete", e));
        }
    }

    private static Map<String, Object> metadataOutput(Metadata metadata) {
        Map<String, Object> output = new HashMap<>();

        output.put("name", metadata.getName());

        if (metadata.getPathLower() != null) {
            output.put("pathLower", metadata.getPathLower());
        }

        if (metadata.getPathDisplay() != null) {
            output.put("pathDisplay", metadata.getPathDisplay());
        }

        return output;
    }
}
