package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxError;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.Map;

public final class TaskResults {

    private TaskResults() {
    }

    public static TaskResult completed(Task task, Map<String, Object> output) {
        TaskResult result = base(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    public static TaskResult failed(Task task, DropboxError error) {
        TaskResult result = base(task);

        result.setStatus(
                error.retryable()
                        ? TaskResult.Status.FAILED
                        : TaskResult.Status.FAILED_WITH_TERMINAL_ERROR
        );

        result.setReasonForIncompletion(error.message());

        Map<String, Object> output = new HashMap<>();
        output.put("errorCode", error.code());
        output.put("errorMessage", error.message());
        output.put("retryable", error.retryable());
        output.put("operation", error.operation());

        result.setOutputData(output);

        return result;
    }

    public static TaskResult invalidInput(
            Task task,
            String operation,
            String message
    ) {
        return failed(
                task,
                new DropboxError(
                        "INVALID_INPUT",
                        message,
                        false,
                        operation
                )
        );
    }

    private static TaskResult base(Task task) {
        TaskResult result = new TaskResult();
        result.setTaskId(task.getTaskId());
        result.setWorkflowInstanceId(task.getWorkflowInstanceId());
        return result;
    }
}
