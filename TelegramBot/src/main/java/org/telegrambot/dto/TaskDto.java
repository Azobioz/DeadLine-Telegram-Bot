package org.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegrambot.model.TelegramUser;


import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {


    private long id;

    private String name;
    private Timestamp deadline;
    private TelegramUser user;

}
