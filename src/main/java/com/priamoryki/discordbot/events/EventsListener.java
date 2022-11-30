package com.priamoryki.discordbot.events;

import com.priamoryki.discordbot.commands.Command;
import com.priamoryki.discordbot.commands.CommandException;
import com.priamoryki.discordbot.commands.CommandsStorage;
import com.priamoryki.discordbot.utils.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Pavel Lymar
 */
public class EventsListener extends ListenerAdapter {
    private final DataSource data;
    private final CommandsStorage commands;

    public EventsListener(DataSource data, CommandsStorage commands) {
        this.data = data;
        this.commands = commands;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is working now!");
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        createGuildAttributes(event.getGuild());
    }

    private void createGuildAttributes(Guild guild) {
        data.executeQuery(String.format("INSERT OR IGNORE INTO servers(server_id) VALUES (%d)", guild.getIdLong()));
        Message message = data.getOrCreateMainMessage(guild);
        data.getOrCreatePlayerMessage(guild);
        try {
            commands.executeCommand("clear_all", message.getGuild(), message.getMember());
        } catch (CommandException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        Guild guild = message.getGuild();
        Member member = message.getMember();
        String messageText = message.getContentDisplay();
        // TODO вот бы это делать более обдуманно
//        if (messageText.equals("create")) {
//            createGuildAttributes(message.getGuild());
//        }
//        data.getOrCreateMainMessage(message.getGuild()).editMessage(MainMessage.getDefaultMessage()).complete();
        try {
            commands.executeCommand(
                    "clear",
                    guild,
                    member,
                    List.of(Long.toString(message.getIdLong()))
            );
        } catch (CommandException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (messageText.startsWith(data.getPrefix()) && !data.isBot(member.getUser())) {
            List<String> splittedMessage = List.of(messageText.substring(data.getPrefix().length()).split(" "));
            Command command = commands.getCommand(splittedMessage.get(0));
            if (command != null && command.isAvailableFromChat()) {
                try {
                    command.executeWithPermissions(guild, member, splittedMessage.subList(1, splittedMessage.size()));
                } catch (CommandException e) {
                    message.reply(e.getMessage()).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                message.reply("Can't find such command!").queue();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Command command = commands.getCommand(event.getName());
        if (command != null && command.isAvailableFromChat()) {
            List<String> args = new ArrayList<>();
            for (OptionData option : command.getOptions()) {
                event.getOptionsByName(option.getName()).stream()
                        .map(OptionMapping::getAsString)
                        .forEach(op -> args.addAll(Arrays.asList(op.split(" "))));
            }
            try {
                command.executeWithPermissions(event.getGuild(), event.getMember(), args);
            } catch (CommandException e) {
                event.reply(e.getMessage()).setEphemeral(true).queue();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        event.reply("DONE!").setEphemeral(true).queue();
    }


    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        try {
            commands.executeCommandWithPermissions(
                    Objects.requireNonNull(event.getButton().getId()).toLowerCase(),
                    event.getGuild(),
                    event.getMember()
            );
        } catch (CommandException e) {
            event.reply(e.getMessage()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
