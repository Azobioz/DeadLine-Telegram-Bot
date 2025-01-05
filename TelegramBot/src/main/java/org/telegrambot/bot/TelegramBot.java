package org.telegrambot.bot;

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

import static org.telegrambot.mapper.TelegramUserMapper.mapToTelegramUser;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final private BotConfig config;
    private Map<Long, BotState> userStates = new HashMap<>();
    TaskDto taskDto = new TaskDto();
    TaskService taskService;
    TelegramUserService userService;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            BotState state = userStates.getOrDefault(chatId, BotState.IDLE);
            String username = update.getMessage().getFrom().getUserName();
            handleMessage(chatId, text, state, username);
        }
    }

    private void handleMessage(long chatId, String text, BotState currentState, String username) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

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
                if (parseToLocalDateTime(text).isBefore(LocalDateTime.now())) {
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
                sendMessage(chatId, "Задача добавлена");
                userStates.put(chatId, BotState.IDLE);
                break;

            case AWAITING_TASK_NAME_TO_DELETE:
                TaskDto taskToDelete = taskService.getTaskByName(text);
                if (taskToDelete == null) {
                    sendMessage(chatId, "Задачи с таким названием нет");
                    userStates.put(chatId, BotState.IDLE);
                }
                else {
                    taskService.deleteTask(taskToDelete);
                    deleteNewTaskMessage(chatId);
                    userStates.put(chatId, BotState.AWAITING_YES_OR_NO);
                    break;
                }
        }

    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (callbackData.equals(YES_BUTTON)) {
            executeEditedMessage(chatId, "Введите название задачи", messageId);
            userStates.put(chatId, BotState.AWAITING_TASK_NAME_TO_DELETE);
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


    private void deleteNewTaskMessage(Long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Задача удалена, удалить еще одну?");

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

    public String timesLeft (LocalDateTime deadline) {

        LocalDate deadlineDate = deadline.toLocalDate();

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDate currentDate = LocalDate.now();
        Duration timesLeft = Duration.between(currentTime, deadline);

        Period period = Period.between(currentDate, deadlineDate);

        long years = period.getYears();
        long months = period.getMonths();
        long days = timesLeft.toDays();
        long hours = timesLeft.toHoursPart();
        long minutes = timesLeft.toMinutesPart();

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
    IDLE, AWAITING_DATE, AWAITING_NAME, AWAITING_YES_OR_NO, AWAITING_TASK_NAME_TO_DELETE
}
