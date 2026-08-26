package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteError;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeleteWorkerTest {

    @Test
    void rejectsMissingPath() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new DeleteWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }

    @Test
    void treatsMissingPathAsSuccessOnRetry() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        DeleteErrorException notFound = new DeleteErrorException(
                "request-id", "path_lookup/not_found/", null, DeleteError.pathLookup(LookupError.NOT_FOUND));
        when(dropbox.files().deleteV2("/obsolete.txt")).thenThrow(notFound);

        Task task = new Task();
        task.setRetryCount(1);
        task.setInputData(Map.of("path", "/obsolete.txt"));

        TaskResult result = new DeleteWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("alreadyDeleted"));
    }
}
