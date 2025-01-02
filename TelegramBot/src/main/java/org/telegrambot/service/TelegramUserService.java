package org.telegrambot.service;

import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.TelegramUser;

public interface TelegramUserService {

    TelegramUserDto findTelegramUserByUsername(String username);
    void saveTelegramUser(TelegramUserDto userDto);
}
