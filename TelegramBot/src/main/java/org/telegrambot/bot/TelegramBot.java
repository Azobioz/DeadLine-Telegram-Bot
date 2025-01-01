package org.telegrambot.bot;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegrambot.config.BotConfig;
import org.telegrambot.model.Task;
import org.telegrambot.model.TelegramUser;
import org.telegrambot.repository.TaskRepository;
import org.telegrambot.repository.UserRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final private BotConfig config;
    private Map<Long, BotState> userStates = new HashMap<>();
    @Autowired
    UserRepository userRepository;
    @Autowired
    TaskRepository taskRepository;



    public TelegramBot(BotConfig config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "about this bot"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            BotState currentState = userStates.getOrDefault(chatId, BotState.IDLE);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            String username = update.getMessage().getChat().getUserName();
            Task task = new Task();
            TelegramUser user = userRepository.findUserByUsername(username);
            switch (currentState) {
                case IDLE:
                    switch (text) {
                        case "/start":
                            startCommandReceived(chatId, username);
                            register(username);
                            break;
                        case "/create":
                            register(username);
                            sendMessage(chatId, "Введите дату (день-месяц-год час-минута)");
                            userStates.put(chatId, BotState.AWAITING_DATE);
                            break;
                    }
                    break;

                case AWAITING_DATE:
                    if (isValidTimestamp(text)) {
                        task.setDeadline(parseToTimestamp(text));
                       
                        task.setUser(user);
                        user.addTask(task);
                        sendMessage(chatId, "Введите название задачи");
                    }
                    else {
                        sendMessage(chatId, "Неверный формат данных");
                        userStates.put(chatId, BotState.IDLE);
                    }
                    userStates.put(chatId, BotState.AWAITING_NAME);
                    break;

                case AWAITING_NAME:

                    Hibernate.initialize(user.getTasks());
                    task.setName(text);
                    user.addTask(task);

                    sendMessage(chatId, "Задача добавлена");
                    userStates.put(chatId, BotState.IDLE);
                    break;
            }


        }
    }

    public Timestamp parseToTimestamp(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(timestamp, formatter);
            return Timestamp.valueOf(localDateTime);
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public boolean isValidTimestamp(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        try {
            LocalDateTime.parse(timestamp, formatter);
            return true;
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public void register(String username) {
        if (userRepository.findUserByUsername(username) == null) {
            TelegramUser user = new TelegramUser();
            user.setUsername(username);
            userRepository.save(user);
        }
    }


    private void startCommandReceived(long chatId, String username) {
        String text = "Hi " + username + ", this is a bot for task management ";
        sendMessage(chatId, text);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message)  {
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }
}

enum BotState {
    IDLE, AWAITING_DATE, AWAITING_NAME
}
