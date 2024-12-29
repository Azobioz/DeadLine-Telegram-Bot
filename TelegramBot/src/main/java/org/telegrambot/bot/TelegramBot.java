package org.telegrambot.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegrambot.config.BotConfig;

@Service
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private BotConfig config;


    @Override
    public String getBotToken() {
            return "";
    }

    @Override
    public void onUpdateReceived(Update update) {

        String chatId = update.getMessage().getChatId().toString(); // Получение id чата

        String text = update.getMessage().getText(); // Получение сообщения

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (text.equals("What's your name?")) {
            message.setText("Example1");
        }
        else message.setText(text);

        try {
            this.execute(message);
        }
        catch (TelegramApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void sendMessage(String text) {

        SendMessage message = new SendMessage();


        try {
            execute(text);
        }
        catch (T)
    }

    @Override
    public String getBotUsername() {
        return "Dl_Dead_Line_bot";
    }
}
