package org.telegrambot.bot;

import com.vdurmont.emoji.EmojiParser;
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
                        createCommandReceived(chatId, userDto);
                        break;
                    case "/check":
                        checkCommandReceived(chatId, userDto);
                        break;
                    case "/delete":
                        deleteCommandReceived(chatId, userDto);
                        break;
                    default:
                        unknownMessageReceived(chatId);
                        break;
                }
                break;

            case AWAITING_DATE:
                awaitingDateBotStateReceived(chatId, userDto, text);
                break;

            case AWAITING_NAME:
                awaitingNameBotStateReceived(chatId, userDto, text);
                break;

            case AWAITING_TASK_NAME_TO_DELETE:
                awaitingTaskNameToDeleteBotStateReceived(chatId, userDto, text);
                break;
        }

    }

    private void sendDelayedMessage(long chatId, TaskDto taskDto, LocalDateTime deadline) {
        LocalDateTime currentTime = LocalDateTime.now();
        Duration differenceBetweenDeadlineAndNow = Duration.between(currentTime, deadline);
        int twelveHoursLeft = (int) differenceBetweenDeadlineAndNow.getSeconds() - ((int) differenceBetweenDeadlineAndNow.getSeconds() - 43200);
        int oneDayLeft = (int) differenceBetweenDeadlineAndNow.getSeconds() - ((int) differenceBetweenDeadlineAndNow.getSeconds() - 86400);

        scheduler.schedule(() -> {
            sendMessage(chatId, "Осталось 12 часов до задачи " + taskDto.getName());

        }, twelveHoursLeft, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            sendMessage(chatId, "Остался день до задачи " + taskDto.getName());
        }, oneDayLeft, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            sendMessage(chatId, "Дедлайн задачи " + taskDto.getName() + " закончился");
        }, (int) differenceBetweenDeadlineAndNow.getSeconds(), TimeUnit.SECONDS);

    }

    private void awaitingTaskNameToDeleteBotStateReceived(long chatId, TelegramUserDto userDto, String text) {

        TaskDto taskToDelete = taskService.getTaskByName(userDto, text);
        if (taskToDelete == null) {
            String mistakeMessage = EmojiParser.parseToUnicode(":x: Задачи с таким названием нет");
            sendMessage(chatId, mistakeMessage);
            userStates.put(chatId, BotState.IDLE);
        }
        else {
            taskService.deleteTask(taskToDelete);
            if (userService.getTelegramUserByUsername(userDto.getUsername()).getTasksDto().isEmpty()) {
                String completedDeleteMessage = EmojiParser.parseToUnicode(":heavy_check_mark: Задача удалена");
                sendMessage(chatId, completedDeleteMessage);
            }
            else {
                String completedDeleteMessage = EmojiParser.parseToUnicode(":heavy_check_mark: Задача удалена, удалить еще одну?");
                textUnderMessage(chatId, completedDeleteMessage);
                userStates.put(chatId, BotState.AWAITING_YES_OR_NO_TO_DELETE_AGAIN);
            }
        }

    }

    private void awaitingNameBotStateReceived(long chatId, TelegramUserDto userDto, String text) {
        taskDto.setName(text);
        userDto.addTask(taskDto);
        taskService.saveTask(taskDto);
        if (userDto.getTasksDto().size() != 15) {
            String completeMessage = EmojiParser.parseToUnicode(":heavy_check_mark: Задача добавлена. Хотите создать еще одну задачу?");
            textUnderMessage(chatId, completeMessage);
            userStates.put(chatId, BotState.AWAITING_YES_OR_NO_TO_CREATE_AGAIN);
        }
        else {
            String completeMessage = EmojiParser.parseToUnicode(":heavy_check_mark: Задача добавлена");
            sendMessage(chatId, completeMessage);
        }
        sendDelayedMessage(chatId, taskDto, taskDto.getDeadline());
    }

    private void awaitingDateBotStateReceived(long chatId, TelegramUserDto userDto, String text) {
        if (parseToLocalDateTime(text) == null) {
            String mistakeMessage = EmojiParser.parseToUnicode(":x: Неверный формат данных");
            sendMessage(chatId, mistakeMessage);
            userStates.put(chatId, BotState.IDLE);
        }
        else if (parseToLocalDateTime(text).isBefore(LocalDateTime.now())) {
            String mistakeMessage = EmojiParser.parseToUnicode(":x: Дата меньше текущей даты");
            sendMessage(chatId, mistakeMessage);
            userStates.put(chatId, BotState.IDLE);
        }
        else if (isValidLocalDateTime(text)) {
            taskDto.setDeadline(parseToLocalDateTime(text));
            taskDto.setUser(mapToTelegramUser(userDto));
            userDto.addTask(taskDto);
            String nameMessage = EmojiParser.parseToUnicode(":pencil: Введите название задачи");
            sendMessage(chatId, nameMessage);
            userStates.put(chatId, BotState.AWAITING_NAME);
        }
    }

    private void unknownMessageReceived(long chatId) {
        String defaultMessage = EmojiParser.parseToUnicode(":x: Нет такой команды");
        sendMessage(chatId, defaultMessage);
    }

    private void deleteCommandReceived(long chatId, TelegramUserDto userDto) {
        if (userDto.getTasksDto().isEmpty()) {
            sendMessage(chatId, "У вас нет задач");
        }
        else {
            String deleteMessage = EmojiParser.parseToUnicode(":pencil: Введите название задачи");
            sendMessage(chatId, deleteMessage, userDto);
            userStates.put(chatId, BotState.AWAITING_TASK_NAME_TO_DELETE);
        }
    }

    private void checkCommandReceived(long chatId, TelegramUserDto userDto) {
        List<TaskDto> list = taskService.getAllTasksByUser(userDto);
        if (list.isEmpty()) {
            sendMessage(chatId, "У вас еще нет задач");
        }
        else {
            for (TaskDto taskDto : list) {
                String checkMessage = EmojiParser.parseToUnicode(":date: " + taskDto.getName() + "\n" + timesLeft(taskDto.getDeadline()));
                sendMessage(chatId, checkMessage);
            }
        }
    }

    private void createCommandReceived(long chatId, TelegramUserDto userDto) {
        if (userDto.getTasksDto().size() == 15) {
            String limitMessage = EmojiParser.parseToUnicode(":card_file_box: Всего можно создать 15 задач");
            sendMessage(chatId, limitMessage);
        }
        else {
            String createMessage = EmojiParser.parseToUnicode(":alarm_clock:  Введите дату (день-месяц-год час:минута)\nНапример 01-05-2025 12:45");
            sendMessage(chatId, createMessage);
            userStates.put(chatId, BotState.AWAITING_DATE);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (callbackData.equals(YES_BUTTON)) {
            TelegramUserDto userDto = userService.getTelegramUserByUsername(callbackQuery.getFrom().getUserName());
            if (userStates.get(chatId).equals(BotState.AWAITING_YES_OR_NO_TO_CREATE_AGAIN) && userDto.getTasksDto().size() != 15) {
                String createMessage = EmojiParser.parseToUnicode(":alarm_clock:  Введите дату (день-месяц-год час:минута)");
                executeEditedMessage(chatId, createMessage, messageId);
                userStates.put(chatId, BotState.AWAITING_DATE);
            }
            else if (userStates.get(chatId).equals(BotState.AWAITING_YES_OR_NO_TO_DELETE_AGAIN)) {
                String nameMessage = EmojiParser.parseToUnicode(":pencil: Введите название задачи");
                executeEditedMessage(chatId, nameMessage, messageId);
                userStates.put(chatId, BotState.AWAITING_TASK_NAME_TO_DELETE);
            }

        }
        else if (callbackData.equals(NO_BUTTON)) {
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

    private void isDeadlineComplete(TaskDto taskDto) {
        LocalDateTime deadline = taskDto.getDeadline();
        LocalDateTime currentTime = LocalDateTime.now();

        if (deadline.isBefore(currentTime) || deadline.equals(currentTime)) {
            taskService.deleteTask(taskDto);
            String deadlineCompletedMessage = EmojiParser.parseToUnicode(":exclamation: Дедлайн задачи " + taskDto.getName() + " закончился");
            sendMessage(taskDto.getUser().getChatId(), deadlineCompletedMessage);
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
            days = period.getDays();
        }
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        String yearWord = "год";
        String monthWord = "месяц";
        String dayWord = "день";
        String hourWord = "час";
        String minutesWord = "минута";
        if (years > 1 && years < 5) {
            yearWord = "года";
        }
        else if (years > 4) {
            yearWord = "лет";
        }
        if (months > 1 && months < 5) {
            monthWord = "месяца";
        }
        else if (months > 4) {
            monthWord = "месяцев";
        }
        if (days > 1 && days < 5) {
            dayWord = "дня";
        }
        else if (days > 4) {
            dayWord = "дней";
        }
        if (hours > 1) {
            hourWord = "часа";
        }
        if (minutes > 1 && minutes < 5) {
            minutesWord = "минуты";
        }
        else if (minutes > 4) {
            minutesWord = "минут";
        }
        return "Осталось: " + years + " " + yearWord + ", " + months + " " + monthWord + ", " + days + " " + dayWord + ", " + hours + " " + hourWord + ", " + minutes + " " + minutesWord;
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
        String text = EmojiParser.parseToUnicode("Здраствуй " + username + " :wave:, это бот, который позволяет создать заметки с фиксированным сроком");
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
    IDLE, AWAITING_DATE, AWAITING_NAME, AWAITING_YES_OR_NO_TO_CREATE_AGAIN, AWAITING_YES_OR_NO_TO_DELETE_AGAIN, AWAITING_TASK_NAME_TO_DELETE
}
