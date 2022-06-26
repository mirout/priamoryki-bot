package com.priamoryki.discordbot.utils;

import com.priamoryki.discordbot.audio.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Pavel Lymar
 */
public class PlayerMessage implements UsefulMessage {
    // TODO here is some bugs with rate limiter. Could be fixed by adding your own rate limiter
    private static final int BLOCKS_NUMBER = 27;
    private static final long MINIMAL_UPDATE_PERIOD = 15_000;
    private final GuildMusicManager guildMusicManager;
    private Message playerMessage;
    private Timer timer;

    public PlayerMessage(GuildMusicManager guildMusicManager) {
        this.guildMusicManager = guildMusicManager;
        createNewMessage();
    }

    private static List<Button> getButtons() {
        List<Button> result = new ArrayList<>();
        result.add(Button.primary("RESUME", "▶"));
        result.add(Button.primary("PAUSE", "⏸"));
        result.add(Button.primary("SKIP", "⏯"));
        result.add(Button.primary("REPEAT", "🔁"));
        result.add(Button.primary("PRINT_QUEUE", "🗒️"));
        return result;
    }

    public static Message getDefaultMessage() {
        return new MessageBuilder().setEmbeds(
                new EmbedBuilder().setColor(Color.BLUE).setTitle("PLAYER MESSAGE").build()
        ).setActionRows(ActionRow.of(getButtons())).build();
    }

    private void createNewMessage() {
        playerMessage = guildMusicManager.getData().getOrCreatePlayerMessage(guildMusicManager.getGuild());
    }

    @Override
    public void update() {
        try {
            playerMessage.editMessage(getMessage()).complete();
        } catch (Exception ignored) {
            createNewMessage();
        }
    }

    public void startUpdateTask() {
        endUpdateTask();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, 0, Math.max(MINIMAL_UPDATE_PERIOD, guildMusicManager.getPlayer().getPlayingTrack().getDuration() / BLOCKS_NUMBER));
    }

    public void endUpdateTask() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
    }

    private Message getMessage() {
        AudioTrack track = guildMusicManager.getPlayer().getPlayingTrack();
        if (track == null) {
            return getDefaultMessage();
        }
        User user = track.getUserData(User.class);
        String url = track.getInfo().uri;
        long currentTime = track.getPosition();
        long duration = track.getDuration();
        int blocks = (int) ((track.getPosition() * BLOCKS_NUMBER) / duration);
        boolean isLive = duration == Long.MAX_VALUE;
        if (isLive) {
            blocks = BLOCKS_NUMBER;
        }
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.BLUE).setAuthor("♪" + user.getName() + "♪");
        if (Utils.isUrl(url)) {
            builder.setTitle(track.getInfo().title, track.getInfo().uri);
        } else {
            builder.setTitle(track.getInfo().title);
        }
        builder.setDescription("🟥".repeat(blocks) + "🟦".repeat(BLOCKS_NUMBER - blocks) + "\n" +
                        (isLive ? "LIVE" : Utils.normalizeTime(currentTime) + " / " + Utils.normalizeTime(duration)))
                .setFooter(guildMusicManager.getMusicParameters().getRepeat() ? "On repeat" : "Not on repeat")
                .setTimestamp(new Date().toInstant());
        return new MessageBuilder().setEmbeds(builder.build())
                .setActionRows(ActionRow.of(getButtons())).build();
    }
}
