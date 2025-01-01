package org.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegrambot.model.TelegramUser;

public interface UserRepository extends JpaRepository<TelegramUser, Long> {
    public TelegramUser findByUsername(String username);
}
