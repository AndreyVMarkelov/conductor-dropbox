package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.Map;

public final class ExtractMarkdownAsyncCheckWorker implements Worker {

    public static final String TASK_NAME = "dropbox_extract_markdown_async_check";

    private static final String OPERATION = "extract_markdown_async_check";

    private final DbxClientV2 dropbox;

    public ExtractMarkdownAsyncCheckWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String asyncJobId = (String) task.getInputData().get("asyncJobId");

        if (asyncJobId == null || asyncJobId.isBlank()) {
            return TaskResults.invalidInput(task, OPERATION, "asyncJobId is required");
        }

        try {
            var result = dropbox.riviera().getMarkdownAsyncCheck(asyncJobId);

            Map<String, Object> output = new HashMap<>();

            if (result.isInProgress()) {
                output.put("status", "in_progress");
                return TaskResults.completed(task, output);
            }

            if (result.isComplete()) {
                var complete = result.getCompleteValue();
                output.put("status", "complete");
                output.put("markdown", complete.getMarkdown());
                return TaskResults.completed(task, output);
            }

            return TaskResults.failed(
                    task,
                    DropboxErrorMapper.map(OPERATION, new IllegalStateException("Unexpected Riviera job status")));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map(OPERATION, e));
        }
    }
}
