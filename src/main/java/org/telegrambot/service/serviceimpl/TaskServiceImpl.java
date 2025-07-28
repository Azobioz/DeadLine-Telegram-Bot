package org.telegrambot.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.Task;
import org.telegrambot.model.TelegramUser;
import org.telegrambot.repository.TaskRepository;
import org.telegrambot.repository.UserRepository;
import org.telegrambot.service.TaskService;

import java.util.List;
import java.util.stream.Collectors;

import static org.telegrambot.mapper.TaskMapper.mapToTask;
import static org.telegrambot.mapper.TaskMapper.mapToTaskDto;
import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

@Service
public class TaskServiceImpl implements TaskService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void saveTask(TaskDto taskDto) {
        taskRepository.save(mapToTask(taskDto));
    }

    @Override
    public TaskDto getTaskByName(TelegramUserDto userDto, String taskName) {
        List<TaskDto> listOfTaskDtos = getAllTasksByUser(userDto);
        for (TaskDto taskDto : listOfTaskDtos) {
            if(taskDto.getName().equals(taskName)) {
                return taskDto;
            }
        }
        return null;
    }

    @Override

    public List<TaskDto> getAllTasksByUser(TelegramUserDto userDto) {
        return taskRepository.getTasksByUser(mapToTelegramUser(userDto))
               .stream()
               .map(task -> mapToTaskDto(task)).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteTask(TaskDto taskDto) {
        Task task = taskRepository.getFirstTaskByName(taskDto.getName());
        TelegramUser user = userRepository.findTelegramUserByUsername(task.getUser().getUsername());
        user.deleteTask(task);
        userRepository.save(user);
        taskRepository.delete(task);
    }

}
