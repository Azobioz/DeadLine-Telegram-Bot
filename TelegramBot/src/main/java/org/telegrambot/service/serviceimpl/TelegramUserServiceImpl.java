package org.telegrambot.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.TelegramUser;
import org.telegrambot.repository.UserRepository;
import org.telegrambot.service.TelegramUserService;

import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;
import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUserDto;

@Service
public class TelegramUserServiceImpl implements TelegramUserService {

    UserRepository userRepository;

    @Autowired
    public TelegramUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TelegramUserDto findTelegramUserByUsername(String username) {
        return mapToTelegramUserDto(userRepository.findTelegramUserByUsername(username));
    }

    @Override
    public void saveTelegramUser(TelegramUserDto userDto) {
        userRepository.save(mapToTelegramUser(userDto));
    }
}
