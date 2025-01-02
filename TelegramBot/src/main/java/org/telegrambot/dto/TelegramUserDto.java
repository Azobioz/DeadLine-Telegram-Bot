package org.telegrambot.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.telegrambot.model.Task;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUserDto {

    private long id;

    private String username;

    private List<TaskDto> tasksDto = new ArrayList<>();

    public void addTask(TaskDto task) {
        tasksDto.add(task);
        task.setUser(mapToTelegramUser(this));
    }

}
