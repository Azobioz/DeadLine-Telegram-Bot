package org.telegrambot.service;

import org.telegrambot.dto.TelegramUserDto;

public interface TelegramUserService {

    TelegramUserDto findTelegramUserByUsername(String username);
    void saveTelegramUser(TelegramUserDto userDto);
}
