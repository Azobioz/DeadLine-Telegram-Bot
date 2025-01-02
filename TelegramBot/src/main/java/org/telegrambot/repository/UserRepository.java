package org.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegrambot.model.TelegramUser;

@Repository
public interface UserRepository extends JpaRepository<TelegramUser, Long> {

    TelegramUser findTelegramUserByUsername(String username);

}
