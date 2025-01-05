package org.telegrambot.mapper;

import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.TelegramUser;

import java.util.stream.Collectors;

import static org.telegrambot.mapper.TaskMapper.mapToTaskDto;

public class TelegramUserMapper {

    public static TelegramUser mapToTelegramUser(TelegramUserDto userDto) {
        return TelegramUser.builder()
                .id(userDto.getId())
                .username(userDto.getUsername())
                .chatId(userDto.getChatId())
                .build();
    }

    public static TelegramUserDto mapToTelegramUserDto(TelegramUser user) {
        return TelegramUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .chatId(user.getChatId())
                .tasksDto(user.getTasks().stream().map(task -> mapToTaskDto(task)).collect(Collectors.toList()))
                .build();
    }

}
