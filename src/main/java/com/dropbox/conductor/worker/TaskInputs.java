package com.dropbox.conductor.worker;

import com.netflix.conductor.common.metadata.tasks.Task;

final class TaskInputs {

    private TaskInputs() {}

    static String string(Task task, String name) {
        Object value = task.getInputData().get(name);
        return value instanceof String string ? string : null;
    }
}
