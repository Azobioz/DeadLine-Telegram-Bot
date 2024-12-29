package org.telegrambot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@PropertySource("application.properties")
@Configuration
public class BotConfig {

    @Value("telegram.bot.name")
    private String name;
    @Value("telegram.bot.token")
    private String token;

}
