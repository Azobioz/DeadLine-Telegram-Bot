package org.telegrambot.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Value;
import org.telegrambot.bot.TelegramBot;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Entity(name="task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Length(max=250)
    private String name;
    @ManyToOne
    @NotNull
    private TelegramUser user;

}
