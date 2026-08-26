package com.dropbox.conductor.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.MetadataV2;
import com.dropbox.core.v2.files.SearchMatchV2;
import com.dropbox.core.v2.files.SearchOptions;
import com.dropbox.core.v2.files.SearchV2Builder;
import com.dropbox.core.v2.files.SearchV2Result;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchWorkerTest {

    @Test
    void returnsSearchMatches() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        SearchV2Builder builder = mock(SearchV2Builder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchV2Builder("report")).thenReturn(builder);

        SearchV2Result searchResult = searchResult(true, "cursor-1");

        when(builder.start()).thenReturn(searchResult);

        Task task = new Task();
        task.setInputData(Map.of("query", "report"));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        assertEquals(true, result.getOutputData().get("hasMore"));
        assertEquals("cursor-1", result.getOutputData().get("cursor"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches =
                (List<Map<String, Object>>) result.getOutputData().get("matches");

        assertEquals(1, matches.size());

        Map<String, Object> first = matches.getFirst();

        assertEquals("file", first.get("type"));
        assertEquals("id:test", first.get("id"));
        assertEquals("report.pdf", first.get("name"));
        assertEquals("/documents/report.pdf", first.get("pathLower"));
        assertEquals("/Documents/report.pdf", first.get("pathDisplay"));
        assertEquals("123456789", first.get("rev"));
        assertEquals(1234L, first.get("size"));

        verify(files).searchV2Builder("report");
        verify(builder).start();
    }

    @Test
    void supportsPathAndMaxResults() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        SearchV2Builder builder = mock(SearchV2Builder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchV2Builder("report")).thenReturn(builder);
        when(builder.withOptions(any(SearchOptions.class))).thenReturn(builder);
        when(builder.start()).thenReturn(new SearchV2Result(List.of(), false));

        Task task = new Task();
        task.setInputData(Map.of(
                "query", "report",
                "path", "/Documents",
                "maxResults", 10));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verify(builder).withOptions(any(SearchOptions.class));
        verify(builder).start();
    }

    @Test
    void continuesSearchFromCursor() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchContinueV2("cursor-1")).thenReturn(searchResult(false, null));

        Task task = new Task();
        task.setInputData(Map.of("cursor", "cursor-1"));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        assertEquals(false, result.getOutputData().get("hasMore"));

        verify(files).searchContinueV2("cursor-1");
        verify(files, never()).searchV2Builder(anyString());
    }

    @Test
    void cursorTakesPrecedenceOverQuery() throws Exception {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchContinueV2("cursor-1")).thenReturn(new SearchV2Result(List.of(), false));

        Task task = new Task();
        task.setInputData(Map.of(
                "cursor", "cursor-1",
                "query", "ignored"));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verify(files).searchContinueV2("cursor-1");
        verify(files, never()).searchV2Builder(anyString());
    }

    @Test
    void rejectsMissingQueryAndCursor() {
        DbxClientV2 dropbox = mock(DbxClientV2.class);

        Task task = new Task();
        task.setInputData(Map.of());

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verifyNoInteractions(dropbox);
    }

    @Test
    void rejectsBlankQuery() {
        DbxClientV2 dropbox = mock(DbxClientV2.class);

        Task task = new Task();
        task.setInputData(Map.of("query", " "));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verifyNoInteractions(dropbox);
    }

    @Test
    void rejectsNonNumericMaxResults() throws DbxException {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        SearchV2Builder builder = mock(SearchV2Builder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchV2Builder("report")).thenReturn(builder);

        Task task = new Task();
        task.setInputData(Map.of(
                "query", "report",
                "maxResults", "ten"));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verify(builder, never()).start();
    }

    @Test
    void rejectsZeroMaxResults() throws DbxException {
        DbxClientV2 dropbox = mock(DbxClientV2.class);
        DbxUserFilesRequests files = mock(DbxUserFilesRequests.class);
        SearchV2Builder builder = mock(SearchV2Builder.class);

        when(dropbox.files()).thenReturn(files);
        when(files.searchV2Builder("report")).thenReturn(builder);

        Task task = new Task();
        task.setInputData(Map.of("query", "report", "maxResults", 0));

        SearchWorker worker = new SearchWorker(dropbox);

        TaskResult result = worker.execute(task);

        assertNotEquals(TaskResult.Status.COMPLETED, result.getStatus());

        verify(builder, never()).start();
    }

    private static SearchV2Result searchResult(boolean hasMore, String cursor) {

        Date now = new Date();

        FileMetadata metadata = FileMetadata.newBuilder("report.pdf", "id:test", now, now, "123456789", 1234L)
                .withPathLower("/documents/report.pdf")
                .withPathDisplay("/Documents/report.pdf")
                .withContentHash("374866b3668401e4d06a652e8cd050f881277b683b06464b17e165dd2b41106c")
                .build();

        SearchMatchV2 match = new SearchMatchV2(MetadataV2.metadata(metadata));

        if (cursor != null) {
            return new SearchV2Result(List.of(match), hasMore, cursor);
        }

        return new SearchV2Result(List.of(match), hasMore);
    }
}
