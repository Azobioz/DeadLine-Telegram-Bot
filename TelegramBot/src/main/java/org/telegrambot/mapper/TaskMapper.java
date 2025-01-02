package org.telegrambot.mapper;

import lombok.Builder;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.model.Task;

public class TaskMapper {

    static public Task mapToTask(TaskDto taskDto) {
        return Task.builder()
                .id(taskDto.getId())
                .name(taskDto.getName())
                .deadline(taskDto.getDeadline())
                .user(taskDto.getUser())
                .build();
    }

    static public TaskDto mapToTaskDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .name(task.getName())
                .deadline(task.getDeadline())
                .user(task.getUser())
                .build();
    }


}
