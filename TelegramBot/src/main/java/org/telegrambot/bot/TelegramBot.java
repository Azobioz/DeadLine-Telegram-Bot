package org.telegrambot.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegrambot.config.BotConfig;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.service.TaskService;
import org.telegrambot.service.TelegramUserService;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final private BotConfig config;
    private Map<Long, BotState> userStates = new HashMap<>();
    TaskDto taskDto = new TaskDto();
    TaskService taskService;
    TelegramUserService userService;

    public TelegramBot(BotConfig config, TelegramUserService userService, TaskService taskService) {
        this.config = config;
        this.userService = userService;
        this.taskService = taskService;
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

            TelegramUserDto userDto = userService.findTelegramUserByUsername(username);

            switch (currentState) {
                case IDLE:
                    switch (text) {
                        case "/start":
                            startCommandReceived(chatId, username);
                            register(username);
                            break;
                        case "/create":
                            register(username);
                            sendMessage(chatId, "Введите дату (день-месяц-год час:минута)");
                            userStates.put(chatId, BotState.AWAITING_DATE);
                            break;
                        case "/check":
                            List<TaskDto> list = taskService.getAllTasksByUser(userDto);
                            for (TaskDto taskDto : list) {
                                sendMessage(chatId, taskDto.getName() + "\n" + timesLeft(taskDto.getDeadline()));
                            }


                    }
                    break;

                case AWAITING_DATE:
                    if (isValidTimestamp(text)) {
                        taskDto.setDeadline(parseToTimestamp(text));
                        taskDto.setUser(mapToTelegramUser(userDto));
                        userDto.addTask(taskDto);
                        sendMessage(chatId, "Введите название задачи");
                        userStates.put(chatId, BotState.AWAITING_NAME);
                    }
                    else {
                        sendMessage(chatId, "Неверный формат данных");
                        userStates.put(chatId, BotState.IDLE);
                    }
                    break;

                case AWAITING_NAME:
                    taskDto.setName(text);
                    userDto.addTask(taskDto);
                    taskService.saveTask(taskDto);
                    sendMessage(chatId, "Задача добавлена");
                    userStates.put(chatId, BotState.IDLE);
                    break;
            }


        }
    }

    public String timesLeft (Timestamp deadline) {
        LocalDateTime deadlineTime = deadline.toLocalDateTime();
        LocalDate deadlineDate = deadline.toLocalDateTime().toLocalDate();
        // Получаем текущее время
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDate currentDate = LocalDate.now();
        Duration timesLeft = Duration.between(currentTime, deadlineTime);
        Period datesLeft = Period.between(currentDate, deadlineDate);
        long days = timesLeft.toDays();
        long hours = timesLeft.toHours();
        long minutes = timesLeft.toMinutes();

        Period period = Period.between(currentDate, deadlineDate);

        return "Осталось: " + period.getYears() + " лет, " + period.getMonths() + " месяцев, " + days + " дней, " + hours + "часов, " + minutes + "минут";
    }

    public Timestamp parseToTimestamp(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
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
        if (userService.findTelegramUserByUsername(username) == null) {
            TelegramUserDto userDto = new TelegramUserDto();
            userDto.setUsername(username);
            userService.saveTelegramUser(userDto);
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
