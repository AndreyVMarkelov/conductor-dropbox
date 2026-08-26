package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.WriteConflictError;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.Map;

public final class CreateFolderWorker implements Worker {

    public static final String TASK_NAME = "dropbox_create_folder";

    private final DbxClientV2 dropbox;

    public CreateFolderWorker(DbxClientV2 dropbox) {
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
            return TaskResults.invalidInput(task, "create_folder", "path is required");
        }

        try {
            CreateFolderResult result = dropbox.files().createFolderV2(path);
            var metadata = result.getMetadata();

            return TaskResults.completed(task, folderOutput(metadata));
        } catch (CreateFolderErrorException e) {
            if (task.getRetryCount() > 0
                    && e.errorValue.isPath()
                    && e.errorValue.getPathValue().isConflict()
                    && e.errorValue.getPathValue().getConflictValue() == WriteConflictError.FOLDER) {
                return TaskResults.completed(task, Map.of("path", path, "alreadyExists", true));
            }
            return TaskResults.failed(task, DropboxErrorMapper.map("create_folder", e));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("create_folder", e));
        }
    }

    private static Map<String, Object> folderOutput(FolderMetadata metadata) {
        Map<String, Object> output = new HashMap<>();
        output.put("id", metadata.getId());
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
