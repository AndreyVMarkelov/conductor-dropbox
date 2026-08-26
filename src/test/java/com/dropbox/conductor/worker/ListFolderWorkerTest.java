package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
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
    void returnsSinglePageAndCursor() throws Exception {

        DbxClientV2 dropbox = mock(DbxClientV2.class);

        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        DbxUserListFolderBuilder builder = mock(DbxUserListFolderBuilder.class);

        when(dropbox.files()).thenReturn(files);

        when(files.listFolderBuilder("/Documents")).thenReturn(builder);

        when(builder.withRecursive(false)).thenReturn(builder);

        when(builder.withIncludeDeleted(false)).thenReturn(builder);

        Date now = new Date();

        FileMetadata file = FileMetadata.newBuilder("a.txt", "id:test", now, now, "123456789", 10L)
                .withPathLower("/documents/a.txt")
                .withPathDisplay("/Documents/a.txt")
                .withContentHash("374866b3668401e4d06a652e8cd050f881277b683b06464b17e165dd2b41106c")
                .build();

        ListFolderResult firstPage = new ListFolderResult(List.of(file), "cursor-1", true);

        when(builder.start()).thenReturn(firstPage);

        Task task = new Task();

        task.setInputData(Map.of("path", "/Documents"));

        ListFolderWorker worker = new ListFolderWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) result.getOutputData().get("entries");

        assertEquals(1, entries.size());

        assertEquals("a.txt", entries.getFirst().get("name"));

        assertEquals(true, result.getOutputData().get("hasMore"));

        assertEquals("cursor-1", result.getOutputData().get("cursor"));

        verify(files, never()).listFolderContinue(anyString());
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

    private static FileMetadata file(String name, String id, String rev) {

        return FileMetadata.newBuilder(name, id, new Date(), new Date(), rev, 1)
                .withPathLower("/input/" + name)
                .withPathDisplay("/input/" + name)
                .build();
    }
}
