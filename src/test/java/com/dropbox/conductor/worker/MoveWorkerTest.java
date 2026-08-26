package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MoveWorkerTest {

    @Test
    void rejectsMissingSourcePath() {
        Task task = new Task();
        task.setInputData(Map.of("toPath", "/destination.txt"));

        TaskResult result = new MoveWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }

    @Test
    void rejectsMissingDestinationPath() {
        Task task = new Task();
        task.setInputData(Map.of("fromPath", "/source.txt"));

        TaskResult result = new MoveWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }
}
