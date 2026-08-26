package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.riviera.FileIdOrUrl;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public final class ExtractMarkdownAsyncStartWorker implements Worker {

    public static final String TASK_NAME = "dropbox_extract_markdown_async_start";

    private static final String OPERATION = "extract_markdown_async_start";

    private final DbxClientV2 dropbox;

    public ExtractMarkdownAsyncStartWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String fileId = TaskInputs.string(task, "fileId");
        if (fileId == null || fileId.isBlank()) {
            return TaskResults.invalidInput(task, OPERATION, "fileId is required");
        }
        if (task.getRetryCount() > 0) {
            return TaskResults.invalidInput(task, OPERATION, "markdown extraction start cannot be safely retried");
        }

        try {
            var result = dropbox.riviera()
                    .getMarkdownAsyncBuilder()
                    .withFileIdOrUrl(FileIdOrUrl.fileId(fileId))
                    .start();
            return TaskResults.completed(task, Map.of("asyncJobId", result.getAsyncJobIdValue()));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map(OPERATION, e));
        }
    }
}
