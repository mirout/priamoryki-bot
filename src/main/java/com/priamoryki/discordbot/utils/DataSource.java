package com.priamoryki.discordbot.utils;

import com.priamoryki.discordbot.commands.Command;
import com.priamoryki.discordbot.commands.CommandsStorage;
import com.priamoryki.discordbot.entities.ServerInfo;
import com.priamoryki.discordbot.events.EventsListener;
import com.priamoryki.discordbot.repositories.ServersRepository;
import com.priamoryki.discordbot.utils.messages.MainMessage;
import com.priamoryki.discordbot.utils.messages.PlayerMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Pavel Lymar
 */
@Component
public class DataSource {
    private final long INVALID_ID = -1;
    private final String TOKEN_ENV_NAME = "TOKEN";
    private final String SETTINGS_PATH = "data/config.json";
    private final ServersRepository serversRepository;
    private final JSONObject settings;
    private JDA bot;

    public DataSource(
            ServersRepository serversRepository
    ) throws IOException, JSONException {
        this.settings = new JSONObject(
                new String(Files.readAllBytes(Paths.get(SETTINGS_PATH)))
        );
        this.serversRepository = serversRepository;
    }

    public void setupBot(CommandsStorage commands) {
        this.bot = JDABuilder.createDefault(getToken())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new EventsListener(this, commands, serversRepository))
                .build();
        var result = commands
                .getCommands()
                .stream()
                .filter(Command::isAvailableFromChat)
                .flatMap(
                    command -> command
                            .getNames()
                            .stream()
                            .map(name -> Utils.commandToSlashCommand(name, command))
                )
                .toList();
        bot.updateCommands()
                .addCommands(result)
                .queue();
    }

    private String parseSetting(String setting) {
        try {
            return settings.getString(setting);
        } catch (JSONException jsonException) {
            return null;
        }
    }

    public String getToken() {
        return System.getenv(TOKEN_ENV_NAME);
    }

    public String getBotName() {
        return parseSetting("bot");
    }

    public long getBotId() {
        return getBot().getSelfUser().getIdLong();
    }

    public String getPrefix() {
        return parseSetting("prefix");
    }

    public long getMainChannelId(long guildId) {
        ServerInfo serverInfo = serversRepository.getServerById(guildId);
        if (serverInfo == null || serverInfo.getChannelId() == null) {
            return INVALID_ID;
        }
        return serverInfo.getChannelId();
    }

    public long getMainMessageId(long guildId) {
        ServerInfo serverInfo = serversRepository.getServerById(guildId);
        if (serverInfo == null || serverInfo.getMessageId() == null) {
            return INVALID_ID;
        }
        return serverInfo.getMessageId();
    }

    public long getPlayerMessageId(long guildId) {
        ServerInfo serverInfo = serversRepository.getServerById(guildId);
        if (serverInfo == null || serverInfo.getPlayerMessageId() == null) {
            return INVALID_ID;
        }
        return serverInfo.getPlayerMessageId();
    }

    public MessageChannel getOrCreateMainChannel(Guild guild) {
        MessageChannel channel = guild.getTextChannelById(getMainChannelId(guild.getIdLong()));
        if (channel == null) {
            channel = guild.createTextChannel(
                    getBotName()
            ).complete();
            ServerInfo serverInfo = serversRepository.getServerById(guild.getIdLong());
            serverInfo.setChannelId(channel.getIdLong());
            serversRepository.update(serverInfo);
        }
        return channel;
    }

    public Message getOrCreateMainMessage(Guild guild) {
        MessageChannel channel = getOrCreateMainChannel(guild);
        Message message = MessageHistory.getHistoryFromBeginning(channel).complete()
                .getMessageById(getMainMessageId(guild.getIdLong()));
        if (message == null) {
            message = channel.sendMessage(
                    MainMessage.fillWithDefaultMessage(new MessageCreateBuilder()).build()
            ).complete();
            ServerInfo serverInfo = serversRepository.getServerById(guild.getIdLong());
            serverInfo.setMessageId(message.getIdLong());
            serversRepository.update(serverInfo);
            message.pin().complete();
        }
        return message;
    }

    public Message getOrCreatePlayerMessage(Guild guild) {
        MessageChannel channel = getOrCreateMainChannel(guild);
        Message message = MessageHistory.getHistoryFromBeginning(channel).complete()
                .getMessageById(getPlayerMessageId(guild.getIdLong()));
        if (message == null) {
            message = channel.sendMessage(
                    PlayerMessage.fillWithDefaultMessage(new MessageCreateBuilder()).build()
            ).complete();
            ServerInfo serverInfo = serversRepository.getServerById(guild.getIdLong());
            serverInfo.setPlayerMessageId(message.getIdLong());
            serversRepository.update(serverInfo);
            message.pin().complete();
        }
        return message;
    }

    public boolean isBot(User user) {
        return user.getIdLong() == getBotId();
    }

    public JDA getBot() {
        return bot;
    }
}
