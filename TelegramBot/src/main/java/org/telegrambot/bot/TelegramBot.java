package org.telegrambot.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegrambot.config.BotConfig;
import org.telegrambot.model.TelegramUser;
import org.telegrambot.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final private BotConfig config;
    private Map<Long, BotState> userStates = new HashMap<>();
    UserRepository userRepository;



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
            switch (currentState) {
                case IDLE:
                    switch (text) {
                        case "/start":
                            startCommandReceived(chatId, username);
                            register(username);
                            break;
                        case "/create":
                            register(username);
                            createCommandReceived(chatId);
                            userStates.put(chatId, BotState.AWAITING_DATE);
                            break;
                    }
                    break;

                case AWAITING_DATE:
                    userStates.put(chatId, BotState.IDLE);
                    break;
            }


        }
    }

    public void register(String username) {
        if (userRepository.findByUsername(username) == null) {
            TelegramUser user = new TelegramUser();
            user.setUsername(username);
            userRepository.save(user);
        }
    }

    private void createCommandReceived(long chatId) {

        sendMessage(chatId, "Введите дату");

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
    IDLE, AWAITING_DATE
}
