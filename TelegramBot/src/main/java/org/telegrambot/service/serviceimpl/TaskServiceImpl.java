package org.telegrambot.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.model.Task;
import org.telegrambot.repository.TaskRepository;
import org.telegrambot.service.TaskService;

import java.util.List;

import static org.telegrambot.mapper.TaskMapper.mapToTask;
import static org.telegrambot.mapper.TaskMapper.mapToTaskDto;

@Service
public class TaskServiceImpl implements TaskService {

    private TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void saveTask(TaskDto taskDto) {
        taskRepository.save(mapToTask(taskDto));
    }

    @Override
    public TaskDto findTaskById(long id) {
        return mapToTaskDto(taskRepository.findById(id).get());
    }
}
