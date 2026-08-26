package com.dropbox.conductor;

import com.dropbox.conductor.dropbox.DropboxClientProvider;
import com.dropbox.conductor.worker.*;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import java.util.List;

public class WorkerApplication {

    public static void main(String[] args) {
        String conductorUrl = System.getenv().getOrDefault("CONDUCTOR_URL", "http://localhost:8080/api");
        var client = new ConductorClient(conductorUrl);
        int workerThreadCount = workerThreadCount();
        var taskClient = new TaskClient(client);

        var dropbox = DropboxClientProvider.create();

        var configurer = new TaskRunnerConfigurer.Builder(
                        taskClient,
                        List.of(
                                new CreateFolderWorker(dropbox),
                                new UploadFileWorker(dropbox),
                                new DownloadFileWorker(dropbox),
                                new MoveWorker(dropbox),
                                new DeleteWorker(dropbox),
                                new ExtractMarkdownAsyncStartWorker(dropbox),
                                new ExtractMarkdownAsyncCheckWorker(dropbox),
                                new ExtractMarkdownWorker(dropbox),
                                new GetMetadataWorker(dropbox),
                                new SearchWorker(dropbox),
                                new ListFolderWorker(dropbox)))
                .withThreadCount(workerThreadCount)
                .build();
        configurer.init();

        System.out.printf("Dropbox workers started: conductor=%s, threads=%d%n", conductorUrl, workerThreadCount);

        Runtime.getRuntime().addShutdownHook(new Thread(configurer::shutdown));
    }

    private static int workerThreadCount() {
        String value = System.getenv().getOrDefault("WORKER_THREAD_COUNT", "2");

        try {
            int count = Integer.parseInt(value);

            if (count <= 0) {
                throw new IllegalArgumentException("WORKER_THREAD_COUNT must be greater than 0");
            }

            return count;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("WORKER_THREAD_COUNT must be a positive integer", e);
        }
    }
}
