package org.telegrambot.bot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegrambot.config.BotConfig;
import org.telegrambot.dto.TaskDto;
import org.telegrambot.dto.TelegramUserDto;
import org.telegrambot.service.TaskService;
import org.telegrambot.service.TelegramUserService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final private BotConfig config;
    private Map<Long, BotState> userStates = new HashMap<>();
    TaskDto taskDto = new TaskDto();
    TaskService taskService;
    TelegramUserService userService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    private boolean isDeadlineIsOneDay;
    private boolean isDeadlineIsTwelveHours;

    public TelegramBot(BotConfig config, TelegramUserService userService, TaskService taskService) {
        this.config = config;
        this.userService = userService;
        this.taskService = taskService;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "about this bot"));
        listOfCommands.add(new BotCommand("/create", "create task"));
        listOfCommands.add(new BotCommand("/check", "check all tasks"));
        listOfCommands.add(new BotCommand("/delete", "delete tasks"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void checkDeadlines() {
        List<TelegramUserDto> users = userService.getAllTelegramUsers();
        for (TelegramUserDto user : users) {
            List<TaskDto> tasks = taskService.getAllTasksByUser(user);
            for (TaskDto task : tasks) {
                isLittleTimeLeft(task, user.getChatId(), task.getDeadline());
            }
        }
    }



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();
        register(username, chatId);
        if (update.hasMessage() && update.getMessage().hasText()) {
            BotState state = userStates.getOrDefault(chatId, BotState.IDLE);
            String text = update.getMessage().getText();
            handleMessage(chatId, text, state, username);
        }

    }

    private void handleMessage(long chatId, String text, BotState currentState, String username) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        TelegramUserDto userDto = userService.getTelegramUserByUsername(username);

        switch (currentState) {
            case IDLE:
                switch (text) {
                    case "/start":
                        startCommandReceived(chatId, username);
                        break;
                    case "/create":
                        sendMessage(chatId, "Введите дату (день-месяц-год час:минута)");
                        userStates.put(chatId, BotState.AWAITING_DATE);
                        break;
                    case "/check":
                        List<TaskDto> list = taskService.getAllTasksByUser(userDto);
                        for (TaskDto taskDto : list) {
                            sendMessage(chatId, taskDto.getName() + "\n" + timesLeft(taskDto.getDeadline()));
                        }
                        break;
                    case "/delete":
                        sendMessage(chatId, "Введите название задачи", userDto);
                        userStates.put(chatId, BotState.AWAITING_TASK_NAME_TO_DELETE);
                        break;
                    default:
                        sendMessage(chatId, "Нет такой команды");
                        break;
                }
                break;

            case AWAITING_DATE:
                if (parseToLocalDateTime(text) == null) {
                    sendMessage(chatId, "Неверный формат данных");
                    userStates.put(chatId, BotState.IDLE);
                }
                else if (parseToLocalDateTime(text).isBefore(LocalDateTime.now())) {
                    sendMessage(chatId, "Дата меньше текущей даты");
                    userStates.put(chatId, BotState.IDLE);
                } else if (isValidLocalDateTime(text)) {
                    taskDto.setDeadline(parseToLocalDateTime(text));
                    taskDto.setUser(mapToTelegramUser(userDto));
                    userDto.addTask(taskDto);
                    sendMessage(chatId, "Введите название задачи");
                    userStates.put(chatId, BotState.AWAITING_NAME);
                }
                break;

            case AWAITING_NAME:
                taskDto.setName(text);
                userDto.addTask(taskDto);
                taskService.saveTask(taskDto);
                textUnderMessage(chatId, "Задача добавлена. Хотите создать еще одну задачу?");
                userStates.put(chatId, BotState.AWAITING_YES_OR_NO);
                break;

            case AWAITING_TASK_NAME_TO_DELETE:
                TaskDto taskToDelete = taskService.getTaskByName(text);
                if (taskToDelete == null) {
                    sendMessage(chatId, "Задачи с таким названием нет");
                    userStates.put(chatId, BotState.IDLE);
                }
                else {
                    taskService.deleteTask(taskToDelete);
                    textUnderMessage(chatId, "Задача удалена, удалить еще одну?");
                    break;
                }

        }

    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (callbackData.equals(YES_BUTTON)) {
            if (userStates.get(chatId).equals(BotState.AWAITING_YES_OR_NO)) {
                executeEditedMessage(chatId, "Введите дату (день-месяц-год час:минута)", messageId);
                userStates.put(chatId, BotState.AWAITING_DATE);
            }
            else {
                executeEditedMessage(chatId, "Введите название задачи", messageId);
                userStates.put(chatId, BotState.AWAITING_TASK_NAME_TO_DELETE);
            }
        } else if (callbackData.equals(NO_BUTTON)) {
            userStates.put(chatId, BotState.IDLE);
            executeEditedMessage(chatId, "Хорошего дня", messageId);
        }
        try {
            execute(new AnswerCallbackQuery(callbackQuery.getId()));
        }
        catch (TelegramApiException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void executeEditedMessage(Long chatId, String text, Integer messageId) {
        EditMessageText editedMessageText = new EditMessageText();
        editedMessageText.setChatId(chatId);
        editedMessageText.setText(text);
        editedMessageText.setMessageId(messageId);
        try {
            execute(editedMessageText);
        }
        catch (TelegramApiException e) {
            System.out.println("Error in executeEditedMessage: " + e);
        }
    }


    private void textUnderMessage(Long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup(); //Встроенная разметка клавиатуры
        List<List<InlineKeyboardButton>> rowsInLine = getButtonsUnderMessage();

        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline); // когда будет отправлено юзеру, то он получит кнопки под текстом

        executeMessage(message);
    }

    private static List<List<InlineKeyboardButton>> getButtonsUnderMessage() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>(); //Список строк кнопок под текстом
        List<InlineKeyboardButton> rowInline = new ArrayList<>(); //строка кнопок
        InlineKeyboardButton yesButton = new InlineKeyboardButton(); //кнопка

        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON); //если пользователь нажмет Yes, то значение callbackData будет YES_BUTTON
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);
        rowInline.add(yesButton); //добавляется в строку кнопок
        rowInline.add(noButton);

        rowsInLine.add(rowInline);
        return rowsInLine;
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup(TelegramUserDto userDto) {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>(); //список всех рядов с кнопками

        KeyboardRow row = new KeyboardRow(); //кнопки в строке

        List<TaskDto> tasks = taskService.getAllTasksByUser(userDto);

        for (int i = 0; i < tasks.size(); i++) {
            if (i % 3 == 0) {
                keyboardRows.add(row);
                row = new KeyboardRow();
            }
            row.add(tasks.get(i).getName());
        }
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private void sendMessage(long chatId, String textToSend, TelegramUserDto userDto) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(getReplyKeyboardMarkup(userDto));

        executeMessage(message);
    }

    public void isLittleTimeLeft(TaskDto taskDto, long chatId, LocalDateTime deadline) {
        LocalDateTime currentTime = LocalDateTime.now();
        Duration duration = Duration.between(currentTime, deadline);

        Period period = Period.between(currentTime.toLocalDate(), deadline.toLocalDate());

        long years = period.getYears();
        long months = period.getMonths();
        long days;

        if (duration.toHours() < 24) {
            days = 0;
        }
        else {
            days = duration.toDays();
        }
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();


        if (years == 0 && months == 0 && days == 0 && hours < 24 && hours > 12 && !isDeadlineIsOneDay) {
            sendMessage(chatId, "Осталось меньше дня до дедлайна задачи " + taskDto.getName());
            isDeadlineIsOneDay = true;
        }
        else if (years == 0 && months == 0 && days == 0 && hours < 12 && hours > 5 && !isDeadlineIsTwelveHours) {
            sendMessage(chatId, "Осталось меньше 12 часов до дедлайна задачи " + taskDto.getName());
            isDeadlineIsTwelveHours = true;
        }
    }

    public String timesLeft(LocalDateTime deadline) {

        LocalDateTime currentTime = LocalDateTime.now();
        Duration duration = Duration.between(currentTime, deadline);

        Period period = Period.between(currentTime.toLocalDate(), deadline.toLocalDate());

        long years = period.getYears();
        long months = period.getMonths();
        long days;

        if (duration.toHours() < 24) {
            days = 0;
        }
        else {
            days = duration.toDays();
        }
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        return "Осталось: " + years + " лет, " + months + " месяцев, " + days + " дней, " + hours + " часов, " + minutes + " минут";
    }

    public LocalDateTime parseToLocalDateTime(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        try {
            return LocalDateTime.parse(text, formatter);
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public boolean isValidLocalDateTime(String timestamp) {
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

    public void register(String username, long chatId) {
        if (userService.getTelegramUserByUsername(username) == null) {
            TelegramUserDto userDto = new TelegramUserDto();
            userDto.setUsername(username);
            userDto.setChatId(chatId);
            userService.saveTelegramUser(userDto);
        }
    }

    private void startCommandReceived(long chatId, String username) {
        String text = "Здраствуй " + username + ", это бот, который позволяет создать заметки с фиксированным сроком";
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
    IDLE, AWAITING_DATE, AWAITING_NAME, AWAITING_YES_OR_NO, AWAITING_TASK_NAME_TO_DELETE
}
