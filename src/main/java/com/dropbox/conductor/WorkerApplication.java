package com.dropbox.conductor;

import com.dropbox.conductor.dropbox.DropboxClientProvider;
import com.dropbox.conductor.worker.*;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import java.util.List;

public class WorkerApplication {

    public static void main(String[] args) {
        var client = new ConductorClient("http://localhost:8080/api");
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
                                new ListFolderWorker(dropbox)))
                .withThreadCount(2)
                .build();
        configurer.init();

        System.out.println("Dropbox workers started");

        Runtime.getRuntime().addShutdownHook(new Thread(configurer::shutdown));
    }
}
