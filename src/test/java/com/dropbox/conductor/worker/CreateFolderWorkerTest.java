package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderError;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.WriteConflictError;
import com.dropbox.core.v2.files.WriteError;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateFolderWorkerTest {

    @Test
    void rejectsMissingPath() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new CreateFolderWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }

    @Test
    void treatsFolderConflictAsSuccessOnRetry() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        CreateFolderErrorException conflict = new CreateFolderErrorException(
                "request-id",
                "path/conflict/folder/",
                null,
                CreateFolderError.path(WriteError.conflict(WriteConflictError.FOLDER)));
        when(dropbox.files().createFolderV2("/processed")).thenThrow(conflict);

        Task task = new Task();
        task.setRetryCount(1);
        task.setInputData(Map.of("path", "/processed"));

        TaskResult result = new CreateFolderWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alreadyExists"));
    }
}
