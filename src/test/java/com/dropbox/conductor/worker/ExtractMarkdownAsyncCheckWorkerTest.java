package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.riviera.DbxUserRivieraRequests;
import com.dropbox.core.v2.riviera.GetMarkdownAsyncCheckResult;
import com.dropbox.core.v2.riviera.GetMarkdownResult;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtractMarkdownAsyncCheckWorkerTest {

    @Test
    void failsWhenAsyncJobIdMissing() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new ExtractMarkdownAsyncCheckWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));

        assertEquals("extract_markdown_async_check", result.getOutputData().get("operation"));
    }

    @Test
    void returnsInProgress() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        GetMarkdownAsyncCheckResult checkResult = mock(GetMarkdownAsyncCheckResult.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncCheck("job-123")).thenReturn(checkResult);
        when(checkResult.isInProgress()).thenReturn(true);

        Task task = new Task();
        task.setInputData(Map.of("asyncJobId", "job-123"));

        TaskResult result = new ExtractMarkdownAsyncCheckWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("in_progress", result.getOutputData().get("status"));
    }

    @Test
    void returnsCompletedMarkdown() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        GetMarkdownAsyncCheckResult checkResult = mock(GetMarkdownAsyncCheckResult.class);
        GetMarkdownResult markdownResult = mock(GetMarkdownResult.class);

        when(riviera.getMarkdownAsyncCheck("job-123")).thenReturn(checkResult);
        when(dropbox.riviera()).thenReturn(riviera);
        when(checkResult.isComplete()).thenReturn(true);
        when(checkResult.getCompleteValue()).thenReturn(markdownResult);
        when(markdownResult.getMarkdown()).thenReturn("# Hello");

        Task task = new Task();
        task.setInputData(Map.of("asyncJobId", "job-123"));

        TaskResult result = new ExtractMarkdownAsyncCheckWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("complete", result.getOutputData().get("status"));
        assertEquals("# Hello", result.getOutputData().get("markdown"));
    }

    @Test
    void mapsDropboxError() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncCheck("job-123")).thenThrow(new RuntimeException("boom"));

        Task task = new Task();
        task.setInputData(Map.of("asyncJobId", "job-123"));

        TaskResult result = new ExtractMarkdownAsyncCheckWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("extract_markdown_async_check", result.getOutputData().get("operation"));
    }
}
