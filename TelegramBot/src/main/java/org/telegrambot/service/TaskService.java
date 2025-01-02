package org.telegrambot.service;

import org.telegrambot.dto.TaskDto;
import org.telegrambot.model.Task;

import java.util.List;

public interface TaskService {

    void saveTask(TaskDto taskDto);
    TaskDto findTaskById(long id);
}
