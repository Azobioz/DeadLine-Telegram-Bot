package org.telegrambot.service.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.model.TelegramUser;
import org.telegrambot.repository.UserRepository;
import org.telegrambot.service.TelegramUserService;

import java.util.List;
import java.util.stream.Collectors;

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
    public TelegramUserDto getTelegramUserByUsername(String username) {
        TelegramUser user = userRepository.findTelegramUserByUsername(username);
        if (user == null) {
            return null;
        }
        return mapToTelegramUserDto(userRepository.findTelegramUserByUsername(username));
    }

    @Override
    public void saveTelegramUser(TelegramUserDto userDto) {
        userRepository.save(mapToTelegramUser(userDto));
    }

    @Override
    @Transactional
    public List<TelegramUserDto> getAllTelegramUsers() {
        return userRepository.findAll().stream().map(user -> mapToTelegramUserDto(user)).collect(Collectors.toList());
    }

    @Override
    public String encodeChatId(Long chatId) {
        return Long.toHexString(chatId);
    }

    @Override
    public Long decodeChatId(String chatId) {
        return Long.parseLong(chatId);
    }


}
