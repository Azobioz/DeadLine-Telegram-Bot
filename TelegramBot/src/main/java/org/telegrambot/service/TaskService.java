package org.telegrambot.service;

import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.TelegramUser;

import java.util.List;

public interface TaskService {

    void saveTask(TaskDto taskDto);
    TaskDto findTaskById(long id);
    List<TaskDto> getAllTasksByUser(TelegramUserDto userDto);
}
