package org.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegrambot.model.Task;
import org.telegrambot.model.TelegramUser;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Task findTaskByUser(TelegramUser user);
}