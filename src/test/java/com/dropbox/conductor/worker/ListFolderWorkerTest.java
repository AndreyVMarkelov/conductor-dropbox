package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.DbxUserListFolderBuilder;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderContinueError;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListFolderWorkerTest {

    @Test
    void failsWhenPathMissing() {
        Task task = new Task();
        task.setInputData(Map.of());

        TaskResult result = new ListFolderWorker(null).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());

        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
    }

    @Test
    void listsSinglePage() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        DbxUserListFolderBuilder builder = mock(DbxUserListFolderBuilder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.listFolderBuilder("/input")).thenReturn(builder);
        when(builder.withRecursive(false)).thenReturn(builder);
        when(builder.withIncludeDeleted(false)).thenReturn(builder);

        FileMetadata file = FileMetadata.newBuilder("hello.txt", "id:file-1", new Date(), new Date(), "abcdef123", 14)
                .withPathLower("/input/hello.txt")
                .withPathDisplay("/input/hello.txt")
                .build();

        FolderMetadata folder = FolderMetadata.newBuilder("archive", "id:folder-1")
                .withPathLower("/input/archive")
                .withPathDisplay("/input/archive")
                .build();

        ListFolderResult page = new ListFolderResult(List.of(file, folder), "cursor-1", false);

        when(builder.start()).thenReturn(page);

        Task task = new Task();
        task.setInputData(Map.of(
                "path", "/input",
                "recursive", false,
                "includeDeleted", false));

        TaskResult result = new ListFolderWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) result.getOutputData().get("entries");

        assertEquals(2, entries.size());
        assertEquals("file", entries.get(0).get("type"));
        assertEquals("folder", entries.get(1).get("type"));
        assertEquals("cursor-1", result.getOutputData().get("cursor"));
        assertEquals(false, result.getOutputData().get("hasMore"));
    }

    @Test
    void listsRootFolder() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        DbxUserListFolderBuilder builder = mock(DbxUserListFolderBuilder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.listFolderBuilder("")).thenReturn(builder);
        when(builder.withRecursive(false)).thenReturn(builder);
        when(builder.withIncludeDeleted(false)).thenReturn(builder);
        when(builder.start()).thenReturn(new ListFolderResult(List.of(), "cursor-root", false));

        Task task = new Task();
        task.setInputData(Map.of("path", "/"));

        TaskResult result = new ListFolderWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        verify(files).listFolderBuilder("");
    }

    @Test
    void continuesListingFromCursor() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        when(dropbox.files()).thenReturn(files);

        ListFolderResult result = new ListFolderResult(List.of(), "next-cursor", false);

        when(files.listFolderContinue("cursor-1")).thenReturn(result);

        Task task = new Task();
        task.setInputData(Map.of("cursor", "cursor-1"));

        ListFolderWorker worker = new ListFolderWorker(dropbox);

        TaskResult taskResult = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, taskResult.getStatus());

        assertEquals(false, taskResult.getOutputData().get("hasMore"));

        verify(files).listFolderContinue("cursor-1");
        verify(files, never()).listFolderBuilder(anyString());
    }

    @Test
    void cursorTakesPrecedenceOverPath() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        when(dropbox.files()).thenReturn(files);

        when(files.listFolderContinue("cursor-1")).thenReturn(new ListFolderResult(List.of(), "next-cursor", false));

        Task task = new Task();
        task.setInputData(Map.of(
                "cursor", "cursor-1",
                "path", "/ignored"));

        ListFolderWorker worker = new ListFolderWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verify(files).listFolderContinue("cursor-1");
        verify(files, never()).listFolderBuilder(anyString());
    }

    @Test
    void returnsStructuredTerminalErrorWhenContinuationCursorIsReset() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        when(dropbox.files()).thenReturn(files);
        when(files.listFolderContinue("expired-cursor"))
                .thenThrow(new ListFolderContinueErrorException(
                        "request-id", "reset/", null, ListFolderContinueError.RESET));

        Task task = new Task();
        task.setInputData(Map.of("cursor", "expired-cursor"));

        TaskResult result = new ListFolderWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("INVALID_INPUT", result.getOutputData().get("errorCode"));
        assertEquals(false, result.getOutputData().get("retryable"));
        assertEquals("list_folder", result.getOutputData().get("operation"));
    }
}
