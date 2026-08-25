package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationResult;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.Map;

public final class MoveWorker implements Worker {

    public static final String TASK_NAME = "dropbox_move";

    private final DbxClientV2 dropbox;

    public MoveWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String fromPath = (String) task.getInputData().get("fromPath");
        String toPath = (String) task.getInputData().get("toPath");
        boolean autorename = Boolean.TRUE.equals(task.getInputData().get("autorename"));

        if (fromPath == null || fromPath.isBlank()) {
            return TaskResults.invalidInput(task, "move", "fromPath is required");
        }

        if (toPath == null || toPath.isBlank()) {
            return TaskResults.invalidInput(task, "move", "toPath is required");
        }

        try {
            RelocationResult relocationResult = dropbox.files()
                    .moveV2Builder(fromPath, toPath)
                    .withAutorename(autorename)
                    .start();

            Metadata metadata = relocationResult.getMetadata();
            return TaskResults.completed(task, metadataOutput(metadata));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("move", e));
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
