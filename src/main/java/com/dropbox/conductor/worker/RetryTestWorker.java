package com.dropbox.conductor.worker;

import com.dropbox.conductor.error.DropboxError;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RetryTestWorker implements Worker {

    public static final String TASK_NAME = "retry_test";

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        int attempt = attempts.computeIfAbsent(task.getWorkflowInstanceId(), ignored -> new AtomicInteger())
                .incrementAndGet();

        if (attempt < 3) {
            return TaskResults.failed(
                    task,
                    new DropboxError("TEMPORARY_UNAVAILABLE", "Simulated transient Dropbox error", true, "retry_test"));
        }

        return TaskResults.completed(task, java.util.Map.of("attempt", attempt, "message", "Recovered after retries"));
    }
}
