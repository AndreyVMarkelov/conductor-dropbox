package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxErrorMapper;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.SearchOptions;
import com.dropbox.core.v2.files.SearchV2Result;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SearchWorker implements Worker {

    public static final String TASK_NAME = "dropbox_search";

    private final DbxClientV2 dropbox;

    public SearchWorker(DbxClientV2 dropbox) {
        this.dropbox = dropbox;
    }

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        String cursor = TaskInputs.string(task, "cursor");

        try {
            if (cursor != null && !cursor.isBlank()) {
                var result = dropbox.files().searchContinueV2(cursor);

                return TaskResults.completed(task, searchOutput(result));
            }

            String query = TaskInputs.string(task, "query");

            if (query == null || query.isBlank()) {
                return TaskResults.invalidInput(task, "search", "query is required when cursor is not provided");
            }

            var builder = dropbox.files().searchV2Builder(query);

            String path = TaskInputs.string(task, "path");

            Object maxResultsValue = task.getInputData().get("maxResults");

            if ((path != null && !path.isBlank()) || maxResultsValue != null) {

                SearchOptions.Builder optionsBuilder = SearchOptions.newBuilder();

                if (path != null && !path.isBlank()) {
                    optionsBuilder.withPath(path);
                }

                if (maxResultsValue != null) {
                    if (!(maxResultsValue instanceof Number number)) {
                        return TaskResults.invalidInput(task, "search", "maxResults must be a number");
                    }

                    long maxResults = number.longValue();

                    if (maxResults <= 0) {
                        return TaskResults.invalidInput(task, "search", "maxResults must be greater than 0");
                    }

                    optionsBuilder.withMaxResults(maxResults);
                }

                builder.withOptions(optionsBuilder.build());
            }

            var result = builder.start();

            return TaskResults.completed(task, searchOutput(result));

        } catch (Exception e) {
            return TaskResults.failed(task, DropboxErrorMapper.map("search", e));
        }
    }

    private static Map<String, Object> searchOutput(SearchV2Result result) {

        List<Map<String, Object>> matches = new ArrayList<>();

        for (var match : result.getMatches()) {
            var metadata = match.getMetadata();

            if (metadata.isMetadata()) {
                matches.add(DropboxMetadataOutput.from(metadata.getMetadataValue()));
            }
        }

        Map<String, Object> output = new HashMap<>();

        output.put("matches", matches);
        output.put("hasMore", result.getHasMore());

        if (result.getCursor() != null) {
            output.put("cursor", result.getCursor());
        }

        return output;
    }
}
