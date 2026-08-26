package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ListFolderWorker implements Worker {

    public static final String TASK_NAME = "dropbox_list_folder";

    private final DbxClientV2 dropbox;

    public ListFolderWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String cursor = (String) task.getInputData().get("cursor");

        try {
            if (cursor != null && !cursor.isBlank()) {
                ListFolderResult result = dropbox.files().listFolderContinue(cursor);

                return TaskResults.completed(task, listFolderOutput(result));
            }

            String path = (String) task.getInputData().get("path");

            if (path == null || path.isBlank()) {
                return TaskResults.invalidInput(task, "list_folder", "path is required when cursor is not provided");
            }

            boolean recursive = Boolean.TRUE.equals(task.getInputData().get("recursive"));

            boolean includeDeleted = Boolean.TRUE.equals(task.getInputData().get("includeDeleted"));

            if ("/".equals(path)) {
                path = "";
            }

            ListFolderResult result = dropbox.files()
                    .listFolderBuilder(path)
                    .withRecursive(recursive)
                    .withIncludeDeleted(includeDeleted)
                    .start();

            return TaskResults.completed(task, listFolderOutput(result));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("list_folder", e));
        }
    }

    private static Map<String, Object> listFolderOutput(ListFolderResult result) {

        List<Map<String, Object>> entries = new ArrayList<>();

        for (Metadata metadata : result.getEntries()) {
            entries.add(DropboxMetadataOutput.from(metadata));
        }

        Map<String, Object> output = new HashMap<>();

        output.put("entries", entries);
        output.put("hasMore", result.getHasMore());

        if (result.getCursor() != null) {
            output.put("cursor", result.getCursor());
        }

        return output;
    }
}
