package org.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private long chatId;
    private List<TaskDto> tasksDto = new ArrayList<>();

    public void addTask(TaskDto task) {
        tasksDto.add(task);
        task.setUser(mapToTelegramUser(this));
    }

}
