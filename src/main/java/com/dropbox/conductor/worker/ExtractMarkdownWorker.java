package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.riviera.FileIdOrUrl;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public final class ExtractMarkdownWorker implements Worker {

    public static final String TASK_NAME = "dropbox_extract_markdown";

    private static final String OPERATION = "extract_markdown";
    private static final int MAX_ATTEMPTS = 30;
    private static final long DEFAULT_POLL_DELAY_MS = 1000;

    private final DbxClientV2 dropbox;
    private final long pollDelayMs;

    public ExtractMarkdownWorker(DbxClientV2 dropbox) {
        this(dropbox, DEFAULT_POLL_DELAY_MS);
    }

    ExtractMarkdownWorker(DbxClientV2 dropbox, long pollDelayMs) {
        this.dropbox = dropbox;
        this.pollDelayMs = pollDelayMs;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String fileId = (String) task.getInputData().get("fileId");

        if (fileId == null || fileId.isBlank()) {
            return TaskResults.invalidInput(task, OPERATION, "fileId is required");
        }

        try {
            var launch = dropbox.riviera()
                    .getMarkdownAsyncBuilder()
                    .withFileIdOrUrl(FileIdOrUrl.fileId(fileId))
                    .start();

            String asyncJobId = launch.getAsyncJobIdValue();

            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                var result = dropbox.riviera().getMarkdownAsyncCheck(asyncJobId);

                if (result.isComplete()) {
                    var markdownResult = result.getCompleteValue();

                    return TaskResults.completed(
                            task, Map.of("asyncJobId", asyncJobId, "markdown", markdownResult.getMarkdown()));
                }

                if (!result.isInProgress()) {
                    return TaskResults.failed(
                            task,
                            DropboxErrorMapper.map(
                                    OPERATION, new IllegalStateException("Unexpected Riviera job status")));
                }

                if (attempt < MAX_ATTEMPTS - 1) {
                    Thread.sleep(pollDelayMs);
                }
            }

            return TaskResults.failed(
                    task,
                    DropboxErrorMapper.map(
                            OPERATION, new IllegalStateException("Timed out waiting for Riviera markdown extraction")));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map(OPERATION, e));
        }
    }
}
