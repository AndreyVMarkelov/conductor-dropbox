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
        String fileId = (String) task.getInputData().get("fileId");
        if (fileId == null || fileId.isBlank()) {
            return TaskResults.invalidInput(task, "extract_markdown_async_start", "fileId is required");
        }

        try {
            var result = dropbox.riviera()
                    .getMarkdownAsyncBuilder()
                    .withFileIdOrUrl(FileIdOrUrl.fileId(fileId))
                    .start();
            return TaskResults.completed(task, Map.of("asyncJobId", result.getAsyncJobIdValue()));
        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("extract_markdown_async_start", e));
        }
    }
}
