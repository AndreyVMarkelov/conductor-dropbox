package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dropbox.conductor.error.DropboxError;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

class TaskResultsTest {

    @Test
    void exposesStableStructuredFieldsForTerminalErrors() {
        Task task = task();

        TaskResult result = TaskResults.invalidInput(task, "upload_file", "path is required");

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("path is required", result.getReasonForIncompletion());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
        assertEquals("path is required", result.getOutputData().get("errorMessage"));
        assertEquals(false, result.getOutputData().get("retryable"));
        assertEquals("upload_file", result.getOutputData().get("operation"));
        assertEquals("task-1", result.getTaskId());
        assertEquals("workflow-1", result.getWorkflowInstanceId());
    }

    @Test
    void keepsTransientErrorsRetryable() {
        TaskResult result =
                TaskResults.failed(task(), new DropboxError("TEMPORARY_UNAVAILABLE", "try again", true, "list_folder"));

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("TEMPORARY_UNAVAILABLE", result.getOutputData().get("errorCode"));
        assertEquals(true, result.getOutputData().get("retryable"));
    }

    private static Task task() {
        Task task = new Task();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("workflow-1");
        return task;
    }
}
