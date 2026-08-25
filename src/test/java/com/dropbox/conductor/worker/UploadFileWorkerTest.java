package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadBuilder;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UploadFileWorkerTest {

    @Test
    void uploadsBase64Content() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class, RETURNS_DEEP_STUBS);
        UploadBuilder builder = mock(UploadBuilder.class);

        FileMetadata metadata = mock(FileMetadata.class);
        when(metadata.getId()).thenReturn("id:test");
        when(metadata.getName()).thenReturn("hello.txt");
        when(metadata.getRev()).thenReturn("rev-1");
        when(metadata.getSize()).thenReturn(5L);
        when(metadata.getPathLower()).thenReturn("/hello.txt");
        when(metadata.getPathDisplay()).thenReturn("/hello.txt");

        when(dropbox.files().uploadBuilder("/hello.txt")).thenReturn(builder);
        when(builder.withMode(any())).thenReturn(builder);
        when(builder.withAutorename(false)).thenReturn(builder);
        when(builder.uploadAndFinish(any(InputStream.class))).thenReturn(metadata);

        Task task = new Task();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("workflow-1");
        task.setInputData(Map.of(
                "path", "/hello.txt",
                "content", "SGVsbG8=",
                "mode", "ADD",
                "autorename", false));

        TaskResult result = new UploadFileWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hello.txt", result.getOutputData().get("name"));
        assertEquals("id:test", result.getOutputData().get("id"));
    }

    @Test
    void failsWhenPathMissing() {
        Task task = new Task();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("workflow-1");
        task.setInputData(Map.of("content", "SGVsbG8="));

        TaskResult result = new UploadFileWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
        assertEquals(false, result.getOutputData().get("retryable"));
        assertEquals("upload_file", result.getOutputData().get("operation"));
        assertEquals("path is required", result.getReasonForIncompletion());
    }
}
