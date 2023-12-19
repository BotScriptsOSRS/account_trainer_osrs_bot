package script.util;

import script.strategy.TaskStrategy;

import java.util.HashMap;
import java.util.Map;

public class TaskRegistry {
    private final Map<Class<? extends TaskStrategy>, TaskStrategy> tasks = new HashMap<>();

    public void registerTask(TaskStrategy task) {
        tasks.put(task.getClass(), task);
    }

    public <T extends TaskStrategy> T getTask(Class<T> taskClass) {
        return taskClass.cast(tasks.get(taskClass));
    }
}

