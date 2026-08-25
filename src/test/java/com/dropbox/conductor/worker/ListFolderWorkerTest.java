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
    void aggregatesMultiplePages() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        DbxUserListFolderBuilder builder = mock(DbxUserListFolderBuilder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.listFolderBuilder("/input")).thenReturn(builder);
        when(builder.withRecursive(false)).thenReturn(builder);
        when(builder.withIncludeDeleted(false)).thenReturn(builder);

        FileMetadata first = file("first.txt", "id:first", "abcdef123");

        FileMetadata second = file("second.txt", "id:second", "abcdef124");

        ListFolderResult firstPage = new ListFolderResult(List.of(first), "cursor-1", true);

        ListFolderResult secondPage = new ListFolderResult(List.of(second), "cursor-2", false);

        when(builder.start()).thenReturn(firstPage);
        when(files.listFolderContinue("cursor-1")).thenReturn(secondPage);

        Task task = new Task();
        task.setInputData(Map.of("path", "/input"));

        TaskResult result = new ListFolderWorker(dropbox).execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) result.getOutputData().get("entries");

        assertEquals(2, entries.size());
        assertEquals("first.txt", entries.get(0).get("name"));
        assertEquals("second.txt", entries.get(1).get("name"));

        verify(files).listFolderContinue("cursor-1");
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

    private static FileMetadata file(String name, String id, String rev) {

        return FileMetadata.newBuilder(name, id, new Date(), new Date(), rev, 1)
                .withPathLower("/input/" + name)
                .withPathDisplay("/input/" + name)
                .build();
    }
}
