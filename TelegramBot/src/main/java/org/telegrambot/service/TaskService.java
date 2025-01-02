package org.telegrambot.service;

import org.telegrambot.dto.TaskDto;

public interface TaskService {

    void saveTask(TaskDto taskDto);
    TaskDto findTaskById(long id);
}
