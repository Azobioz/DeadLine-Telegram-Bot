package org.telegrambot.service;

import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;

import java.util.List;

public interface TaskService {

    void saveTask(TaskDto taskDto);
    List<TaskDto> getAllTasksByUser(TelegramUserDto userDto);
    void deleteTask(TaskDto taskDto);
    TaskDto getTaskByName(TelegramUserDto userDto, String taskName);
}
