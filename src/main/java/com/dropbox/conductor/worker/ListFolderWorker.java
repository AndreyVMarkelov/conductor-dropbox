package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
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
        String path = (String) task.getInputData().get("path");
        boolean recursive = Boolean.TRUE.equals(task.getInputData().get("recursive"));
        boolean includeDeleted = Boolean.TRUE.equals(task.getInputData().get("includeDeleted"));

        if (path == null || path.isBlank()) {
            return TaskResults.invalidInput(task, "list_folder", "path is required");
        }

        if ("/".equals(path)) {
            path = "";
        }

        try {
            ListFolderResult result = dropbox.files()
                    .listFolderBuilder(path)
                    .withRecursive(recursive)
                    .withIncludeDeleted(includeDeleted)
                    .start();

            List<Map<String, Object>> entries = new ArrayList<>();

            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    entries.add(DropboxMetadataOutput.from(metadata));
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = dropbox.files().listFolderContinue(result.getCursor());
            }

            return TaskResults.completed(
                    task,
                    Map.of(
                            "entries", entries,
                            "cursor", result.getCursor(),
                            "hasMore", result.getHasMore()));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("list_folder", e));
        }
    }
}
