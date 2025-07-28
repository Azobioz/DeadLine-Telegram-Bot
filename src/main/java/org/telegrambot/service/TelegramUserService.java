package org.telegrambot.service;

import org.telegrambot.dto.TelegramUserDto;

import java.util.List;

public interface TelegramUserService {

    TelegramUserDto getTelegramUserByUsername(String username);
    void saveTelegramUser(TelegramUserDto userDto);
    List<TelegramUserDto> getAllTelegramUsers();
    String encodeChatId(Long chatId);
    Long decodeChatId(String chatId);
}
