package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.async.LaunchResultBase;
import com.dropbox.core.v2.riviera.DbxUserGetMarkdownAsyncBuilder;
import com.dropbox.core.v2.riviera.DbxUserRivieraRequests;
import com.dropbox.core.v2.riviera.GetMarkdownAsyncCheckResult;
import com.dropbox.core.v2.riviera.GetMarkdownResult;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtractMarkdownWorkerTest {

    @Test
    void failsWhenFileIdMissing() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new ExtractMarkdownWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));

        assertEquals("extract_markdown", result.getOutputData().get("operation"));
    }

    @Test
    void returnsMarkdownWhenJobCompletes() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        DbxUserGetMarkdownAsyncBuilder builder = mock(DbxUserGetMarkdownAsyncBuilder.class);

        LaunchResultBase launchResult = mock(LaunchResultBase.class);
        GetMarkdownAsyncCheckResult checkResult = mock(GetMarkdownAsyncCheckResult.class);
        GetMarkdownResult markdownResult = mock(GetMarkdownResult.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncBuilder()).thenReturn(builder);
        when(builder.withFileIdOrUrl(any())).thenReturn(builder);
        when(builder.start()).thenReturn(launchResult);
        when(launchResult.getAsyncJobIdValue()).thenReturn("job-123");

        when(riviera.getMarkdownAsyncCheck("job-123")).thenReturn(checkResult);
        when(checkResult.isComplete()).thenReturn(true);
        when(checkResult.getCompleteValue()).thenReturn(markdownResult);
        when(markdownResult.getMarkdown()).thenReturn("# Hello");

        Task task = new Task();
        task.setInputData(Map.of("fileId", "id:JXEq1VkznpQAAAAAAAAERg"));

        TaskResult result = new ExtractMarkdownWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("job-123", result.getOutputData().get("asyncJobId"));
        assertEquals("# Hello", result.getOutputData().get("markdown"));
    }

    @Test
    void waitsWhileJobIsInProgressThenCompletes() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        DbxUserGetMarkdownAsyncBuilder builder = mock(DbxUserGetMarkdownAsyncBuilder.class);

        LaunchResultBase launchResult = mock(LaunchResultBase.class);

        GetMarkdownAsyncCheckResult inProgress = mock(GetMarkdownAsyncCheckResult.class);
        GetMarkdownAsyncCheckResult complete = mock(GetMarkdownAsyncCheckResult.class);
        GetMarkdownResult markdownResult = mock(GetMarkdownResult.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncBuilder()).thenReturn(builder);
        when(builder.withFileIdOrUrl(any())).thenReturn(builder);
        when(builder.start()).thenReturn(launchResult);
        when(launchResult.getAsyncJobIdValue()).thenReturn("job-123");

        when(riviera.getMarkdownAsyncCheck("job-123")).thenReturn(inProgress, complete);

        when(inProgress.isComplete()).thenReturn(false);
        when(inProgress.isInProgress()).thenReturn(true);

        when(complete.isComplete()).thenReturn(true);
        when(complete.getCompleteValue()).thenReturn(markdownResult);
        when(markdownResult.getMarkdown()).thenReturn("# Done");

        Task task = new Task();
        task.setInputData(Map.of("fileId", "id:file-1"));

        TaskResult result = new ExtractMarkdownWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("# Done", result.getOutputData().get("markdown"));

        verify(riviera, times(2)).getMarkdownAsyncCheck("job-123");
    }

    @Test
    void mapsLaunchError() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserRivieraRequests riviera = mock(DbxUserRivieraRequests.class);
        DbxUserGetMarkdownAsyncBuilder builder = mock(DbxUserGetMarkdownAsyncBuilder.class);

        when(dropbox.riviera()).thenReturn(riviera);
        when(riviera.getMarkdownAsyncBuilder()).thenReturn(builder);
        when(builder.withFileIdOrUrl(any())).thenReturn(builder);
        when(builder.start()).thenThrow(new RuntimeException("boom"));

        Task task = new Task();
        task.setInputData(Map.of("fileId", "id:file-1"));

        TaskResult result = new ExtractMarkdownWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("extract_markdown", result.getOutputData().get("operation"));
    }
}
