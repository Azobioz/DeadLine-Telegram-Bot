package org.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.telegrambot.model.Task;
import org.telegrambot.model.TelegramUser;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Transactional
    List<Task> getTasksByUser(TelegramUser user);
    Task getFirstTaskByName(String taskName);
}
