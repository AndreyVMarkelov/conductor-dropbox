package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.async.LaunchResultBase;
import com.dropbox.core.v2.riviera.DbxUserGetMarkdownAsyncBuilder;
import com.dropbox.core.v2.riviera.DbxUserRivieraRequests;
import com.dropbox.core.v2.riviera.FileIdOrUrl;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtractMarkdownAsyncStartWorkerTest {

    @Test
    void failsWhenFileIdMissing() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new ExtractMarkdownAsyncStartWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));

        assertEquals("extract_markdown_async_start", result.getOutputData().get("operation"));
    }

    @Test
    void rejectsRetriesBecauseStartingAnotherJobIsUnsafe() {
        Task task = new Task();
        task.setRetryCount(1);
        task.setInputData(Map.of("fileId", "id:file-1"));

        TaskResult result = new ExtractMarkdownAsyncStartWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
        assertEquals("markdown extraction start cannot be safely retried", result.getReasonForIncompletion());
    }

    @Test
    void startsMarkdownExtraction() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        DbxUserGetMarkdownAsyncBuilder builder = mock(DbxUserGetMarkdownAsyncBuilder.class);

        LaunchResultBase launchResult = mock(LaunchResultBase.class);
        when(builder.start()).thenReturn(launchResult);
        when(launchResult.getAsyncJobIdValue()).thenReturn("job-123");

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncBuilder()).thenReturn(builder);
        when(builder.withFileIdOrUrl(any(FileIdOrUrl.class))).thenReturn(builder);
        when(builder.start()).thenReturn(launchResult);
        when(launchResult.getAsyncJobIdValue()).thenReturn("job-123");

        Task task = new Task();
        task.setInputData(Map.of("fileId", "id:JXEq1VkznpQAAAAAAAAERg"));

        TaskResult result = new ExtractMarkdownAsyncStartWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("job-123", result.getOutputData().get("asyncJobId"));

        verify(riviera).getMarkdownAsyncBuilder();
        verify(builder).withFileIdOrUrl(any(FileIdOrUrl.class));
        verify(builder).start();
    }

    @Test
    void mapsDropboxError() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        DbxUserGetMarkdownAsyncBuilder builder = mock(DbxUserGetMarkdownAsyncBuilder.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncBuilder()).thenReturn(builder);
        when(builder.withFileIdOrUrl(any(FileIdOrUrl.class))).thenReturn(builder);
        when(builder.start()).thenThrow(new RuntimeException("boom"));

        Task task = new Task();
        task.setInputData(Map.of("fileId", "id:JXEq1VkznpQAAAAAAAAERg"));

        TaskResult result = new ExtractMarkdownAsyncStartWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("extract_markdown_async_start", result.getOutputData().get("operation"));
    }
}
