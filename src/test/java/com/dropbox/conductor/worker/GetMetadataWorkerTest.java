package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetMetadataWorkerTest {

    @Test
    void returnsFileMetadata() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        when(dropbox.files()).thenReturn(files);

        Date now = new Date();

        FileMetadata metadata = FileMetadata.newBuilder("report.pdf", "id:test", now, now, "123456789", 1234L)
                .withPathLower("/documents/report.pdf")
                .withPathDisplay("/Documents/report.pdf")
                .withContentHash("374866b3668401e4d06a652e8cd050f881277b683b06464b17e165dd2b41106c")
                .build();

        when(files.getMetadata("/Documents/report.pdf")).thenReturn(metadata);

        Task task = new Task();
        task.setInputData(Map.of("path", "/Documents/report.pdf"));

        GetMetadataWorker worker = new GetMetadataWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        assertEquals("file", result.getOutputData().get("type"));
        assertEquals("id:test", result.getOutputData().get("id"));
        assertEquals("report.pdf", result.getOutputData().get("name"));
        assertEquals("/documents/report.pdf", result.getOutputData().get("pathLower"));
        assertEquals("/Documents/report.pdf", result.getOutputData().get("pathDisplay"));
        assertEquals("123456789", result.getOutputData().get("rev"));
        assertEquals(1234L, result.getOutputData().get("size"));
        assertEquals(
                "374866b3668401e4d06a652e8cd050f881277b683b06464b17e165dd2b41106c",
                result.getOutputData().get("contentHash"));

        verify(files).getMetadata("/Documents/report.pdf");
    }

    @Test
    void rejectsMissingPath() {
        DbxClientV2 dropbox = mock(DbxClientV2.class);

        Task task = new Task();
        task.setInputData(Map.of());

        GetMetadataWorker worker = new GetMetadataWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verifyNoInteractions(dropbox);
    }

    @Test
    void rejectsBlankPath() {
        DbxClientV2 dropbox = mock(DbxClientV2.class);

        Task task = new Task();
        task.setInputData(Map.of("path", " "));

        GetMetadataWorker worker = new GetMetadataWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verifyNoInteractions(dropbox);
    }

    @Test
    void rejectsNonStringPath() {
        Task task = new Task();
        task.setInputData(Map.of("path", 42));

        TaskResult result = new GetMetadataWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }
}
