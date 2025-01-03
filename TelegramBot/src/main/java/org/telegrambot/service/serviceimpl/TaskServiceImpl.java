package org.telegrambot.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.repository.TaskRepository;
import org.telegrambot.service.TaskService;

import java.util.List;
import java.util.stream.Collectors;

import static org.telegrambot.mapper.TaskMapper.mapToTask;
import static org.telegrambot.mapper.TaskMapper.mapToTaskDto;
import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

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

    @Override

    public List<TaskDto> getAllTasksByUser(TelegramUserDto user) {
       List<TaskDto> list =  taskRepository.getTasksByUser(mapToTelegramUser(user)).stream().map(task -> mapToTaskDto(task)).collect(Collectors.toList());
       return list;
    }
}
