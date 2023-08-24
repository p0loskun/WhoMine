package com.minersstudios.msessentials.util;

import com.minersstudios.mscore.MSCore;
import com.minersstudios.mscore.util.Badges;
import com.minersstudios.msessentials.MSEssentials;
import com.minersstudios.msessentials.Config;
import com.minersstudios.msessentials.player.PlayerInfo;
import com.minersstudios.msessentials.world.WorldDark;
import com.minersstudios.mscore.plugin.config.LanguageFile;
import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.mscore.util.ChatUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.EmbedType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.minersstudios.msessentials.util.MessageUtils.Colors.*;
import static github.scarsz.discordsrv.util.DiscordUtil.getTextChannelById;
import static github.scarsz.discordsrv.util.DiscordUtil.sendMessage;
import static net.kyori.adventure.text.Component.*;

public final class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Sends message to all players except those in world_dark
     *
     * @param message message
     */
    public static void sendGlobalMessage(final @NotNull Component message) {
        for (final var player : Bukkit.getOnlinePlayers()) {
            if (!WorldDark.isInWorldDark(player)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Sends message to all players within the specified radius
     *
     * @param message  message
     * @param location center location
     * @param radius   radius
     */
    public static void sendLocalMessage(
            final @NotNull Component message,
            final @NotNull Location location,
            final double radius
    ) {
        MSEssentials.getInstance().runTask(
                () -> location.getWorld().getNearbyPlayers(location, radius)
                        .forEach(player -> player.sendMessage(message))
        );
    }

    /**
     * Sends message to chat
     *
     * @param playerInfo player info
     * @param location   sender location
     * @param chat       chat
     * @param message    message
     */
    public static void sendMessageToChat(
            final @NotNull PlayerInfo playerInfo,
            final @Nullable Location location,
            final @NotNull Chat chat,
            final @NotNull Component message
    ) {
        final Config config = MSEssentials.getConfiguration();

        if (chat == Chat.LOCAL && location != null) {
            final Component localMessage = space()
                    .append(playerInfo.getDefaultName()
                    .append(text(" : "))
                    .color(CHAT_COLOR_PRIMARY)
                    .hoverEvent(HoverEvent.showText(text("Нажмите, чтобы написать приватное сообщение данному игроку", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.suggestCommand("/pm " + playerInfo.getID() + " ")))
                    .append(message)
                    .color(CHAT_COLOR_SECONDARY);
            final String stringLocalMessage = ChatUtils.serializeLegacyComponent(localMessage);

            sendLocalMessage(localMessage, location, config.localChatRadius);
            MSEssentials.getInstance().runTaskAsync(
                    () -> sendMessage(getTextChannelById(config.discordLocalChannelId), stringLocalMessage)
            );
            MSLogger.info(localMessage);
            return;
        }

        final Component globalMessage = space()
                .append(text("[WM] ")
                .append(playerInfo.getDefaultName()
                .append(text(" : ")))
                .color(CHAT_COLOR_PRIMARY)
                .hoverEvent(HoverEvent.showText(text("Нажмите, чтобы написать приватное сообщение данному игроку", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.suggestCommand("/pm " + playerInfo.getID() + " ")))
                .append(message)
                .color(CHAT_COLOR_SECONDARY);
        final String stringGlobalMessage = ChatUtils.serializeLegacyComponent(globalMessage);

        sendGlobalMessage(globalMessage);
        MSCore.getInstance().runTaskAsync(() -> {
            sendMessage(getTextChannelById(config.discordGlobalChannelId), stringGlobalMessage.replaceFirst("\\[WM]", ""));
            sendMessage(getTextChannelById(config.discordLocalChannelId), stringGlobalMessage);
        });
        MSLogger.info(globalMessage);
    }

    /**
     * Sends private message
     *
     * @param sender   private message sender
     * @param receiver private message receiver
     * @param message  private message
     * @return True if sender or receiver == null
     */
    public static boolean sendPrivateMessage(
            final @NotNull PlayerInfo sender,
            final @NotNull PlayerInfo receiver,
            final @NotNull Component message
    ) {
        final CommandSender commandSender = sender == MSEssentials.getConsolePlayerInfo()
                ? Bukkit.getConsoleSender()
                : sender.getOnlinePlayer();
        final Player receiverPlayer = receiver.getOnlinePlayer();

        if (commandSender != null && receiverPlayer != null) {
            final Component privateMessage = space()
                    .append(sender.getDefaultName()
                    .append(text(" -> ")
                    .append(receiver.getDefaultName()
                    .append(text(" : ")))))
                    .color(CHAT_COLOR_PRIMARY)
                    .append(message.color(CHAT_COLOR_SECONDARY));
            final String privateMessageString = ChatUtils.serializeLegacyComponent(privateMessage);

            commandSender.sendMessage(
                    Badges.SPEECH.append(text()
                    .append(text("Вы -> ")
                    .append(receiver.getDefaultName()
                    .append(text(" : ")))
                    .hoverEvent(HoverEvent.showText(text("Нажмите, чтобы написать приватное сообщение данному игроку", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.suggestCommand("/pm " + receiver.getID() + " ")))
                    .color(CHAT_COLOR_PRIMARY))
                    .append(message.color(CHAT_COLOR_SECONDARY))
            );
            receiverPlayer.sendMessage(
                    Badges.SPEECH.append(sender.getDefaultName().append(text(" -> Вам : "))
                    .color(CHAT_COLOR_PRIMARY)
                    .hoverEvent(HoverEvent.showText(text("Нажмите, чтобы написать приватное сообщение данному игроку", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.suggestCommand("/pm " + sender.getID() + " ")))
                    .append(message.color(CHAT_COLOR_SECONDARY))
            );
            MSCore.getInstance().runTaskAsync(
                    () -> sendMessage(getTextChannelById(MSEssentials.getConfiguration().discordLocalChannelId), privateMessageString)
            );
            MSLogger.info(privateMessage);
            return true;
        }
        return false;
    }

    /**
     * Sends rp event message to chat
     *
     * @param sender             player
     * @param speech             speech
     * @param action             action
     * @param rolePlayActionType rp action type
     */
    public static void sendRPEventMessage(
            final @NotNull Player sender,
            final @NotNull Component speech,
            final @NotNull Component action,
            final @NotNull RolePlayActionType rolePlayActionType
    ) {
        final Config config = MSEssentials.getConfiguration();
        final PlayerInfo playerInfo = PlayerInfo.fromOnlinePlayer(sender);
        final Component fullMessage = switch (rolePlayActionType) {
            case DO ->
                    text("* ", RP_MESSAGE_MESSAGE_COLOR_PRIMARY)
                    .append(action.color(RP_MESSAGE_MESSAGE_COLOR_SECONDARY))
                    .append(text(" * | ", RP_MESSAGE_MESSAGE_COLOR_PRIMARY))
                    .append(playerInfo.getGrayIDGoldName());
            case IT ->
                    text("* ", RP_MESSAGE_MESSAGE_COLOR_PRIMARY)
                    .append(action.color(RP_MESSAGE_MESSAGE_COLOR_SECONDARY))
                    .append(text(" *", RP_MESSAGE_MESSAGE_COLOR_PRIMARY));
            case TODO ->
                    text("* ")
                    .color(RP_MESSAGE_MESSAGE_COLOR_PRIMARY)
                    .append(speech
                    .color(RP_MESSAGE_MESSAGE_COLOR_SECONDARY))
                    .append(text(" - ")
                    .append(playerInfo.getPlayerFile().getPronouns().getSaidMessage()))
                    .color(RP_MESSAGE_MESSAGE_COLOR_PRIMARY)
                    .append(space())
                    .append(playerInfo.getGrayIDGoldName())
                    .append(text(", ", RP_MESSAGE_MESSAGE_COLOR_PRIMARY))
                    .append(action
                    .color(RP_MESSAGE_MESSAGE_COLOR_SECONDARY))
                    .append(text(" *", RP_MESSAGE_MESSAGE_COLOR_PRIMARY));
            default ->
                    text("* ", RP_MESSAGE_MESSAGE_COLOR_PRIMARY)
                    .append(playerInfo.getGrayIDGoldName())
                    .append(space()
                    .append(action.color(RP_MESSAGE_MESSAGE_COLOR_SECONDARY)))
                    .append(text(" *", RP_MESSAGE_MESSAGE_COLOR_PRIMARY));
        };

        sendLocalMessage(Badges.YELLOW_EXCLAMATION_MARK.append(fullMessage), sender.getLocation(), config.localChatRadius);
        MSCore.getInstance().runTaskAsync(
                () -> sendMessage(getTextChannelById(config.discordLocalChannelId), ChatUtils.serializeLegacyComponent(fullMessage))
        );
        MSLogger.info(fullMessage);
    }

    public static void sendRPEventMessage(
            final @NotNull Player player,
            final @NotNull Component action,
            final @NotNull RolePlayActionType rolePlayActionType
    ) {
        sendRPEventMessage(player, Component.empty(), action, rolePlayActionType);
    }

    /**
     * Sends death message
     *
     * @param killed killed player
     * @param killer killer player
     */
    public static void sendDeathMessage(
            final @NotNull Player killed,
            final @Nullable Player killer
    ) {
        final Config config = MSEssentials.getConfiguration();
        final Location deathLocation = killed.getLocation();
        final PlayerInfo killedInfo = PlayerInfo.fromOnlinePlayer(killed);
        final PlayerInfo killerInfo = killer != null
                        ? PlayerInfo.fromOnlinePlayer(killer)
                        : null;
        final Component deathMessage = killerInfo != null
                ? space()
                .append(killerInfo.getGoldenName()
                .append(space()))
                .append(killerInfo.getPlayerFile().getPronouns().getKillMessage()
                .color(JOIN_MESSAGE_COLOR_PRIMARY)
                .append(space()))
                .append(killedInfo.getGoldenName())
                : space()
                .append(killedInfo.getGoldenName()
                .append(space()))
                .append(killedInfo.getPlayerFile().getPronouns().getDeathMessage())
                .color(JOIN_MESSAGE_COLOR_PRIMARY);
        final String stringDeathMessage = ChatUtils.serializeLegacyComponent(deathMessage);

        killedInfo.setLastDeathLocation(deathLocation);
        sendGlobalMessage(deathMessage);
        MSCore.getInstance().runTaskAsync(() -> {
            sendActionMessage(killed, getTextChannelById(config.discordGlobalChannelId), stringDeathMessage, 16757024);
            sendActionMessage(killed, getTextChannelById(config.discordLocalChannelId), stringDeathMessage, 16757024);
        });
        MSLogger.info(deathMessage);

        MSLogger.info(
                translatable(
                        "ms.info.player_death_info",
                        killedInfo.getDefaultName(),
                        text(killed.getName()),
                        text(deathLocation.getBlock().getWorld().getName()),
                        text(
                                deathLocation.getBlockX() + " "
                                + deathLocation.getBlockY() + " "
                                + deathLocation.getBlockZ()
                        )
                )
        );
    }

    /**
     * Sends join message
     *
     * @param playerInfo playerInfo
     */
    public static void sendJoinMessage(final @NotNull PlayerInfo playerInfo) {
        final Player player = playerInfo.getOnlinePlayer();

        if (
                !playerInfo.isOnline(true)
                || player == null
        ) return;

        final Config config = MSEssentials.getConfiguration();
        final Component joinMessage = space()
                .append(playerInfo.getGoldenName()
                .append(space()))
                .append(playerInfo.getPlayerFile().getPronouns().getJoinMessage())
                .color(JOIN_MESSAGE_COLOR_PRIMARY);
        final String stringJoinMessage = ChatUtils.serializeLegacyComponent(joinMessage);

        sendGlobalMessage(joinMessage);
        MSCore.getInstance().runTaskAsync(() -> {
            sendActionMessage(player, getTextChannelById(config.discordGlobalChannelId), stringJoinMessage, 65280);
            sendActionMessage(player, getTextChannelById(config.discordLocalChannelId), stringJoinMessage, 65280);
        });
        MSLogger.info(joinMessage);
    }

    /**
     * Sends leave message
     *
     * @param playerInfo playerInfo
     * @param player     player
     */
    public static void sendQuitMessage(
            final @NotNull PlayerInfo playerInfo,
            final @NotNull Player player
    ) {
        if (!playerInfo.isOnline()) return;

        final Config config = MSEssentials.getConfiguration();
        final Component quitMessage = space()
                .append(playerInfo.getGoldenName()
                .append(space()))
                .append(playerInfo.getPlayerFile().getPronouns().getQuitMessage())
                .color(JOIN_MESSAGE_COLOR_PRIMARY);
        final String stringQuitMessage = ChatUtils.serializeLegacyComponent(quitMessage);

        sendGlobalMessage(quitMessage);
        MSCore.getInstance().runTaskAsync(() -> {
           sendActionMessage(player, getTextChannelById(config.discordGlobalChannelId), stringQuitMessage, 16711680);
           sendActionMessage(player, getTextChannelById(config.discordLocalChannelId), stringQuitMessage, 16711680);
        });
        MSLogger.info(quitMessage);
    }

    public static @NotNull MessageEmbed craftEmbed(final @NotNull String description) {
        return new MessageEmbed(
                null,
                LanguageFile.renderTranslation("ms.discord.embed.title"),
                description,
                EmbedType.RICH,
                null,
                0x3368cb,
                new MessageEmbed.Thumbnail(
                        "https://github.com/MinersStudios/WhoMine/blob/release/assets/logo/text_logo.png?raw=true",
                        null,
                        0,
                        0
                ),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static void sendActionMessage(
            final @NotNull Player player,
            final TextChannel textChannel,
            final @NotNull String actionMessage,
            final int colorRaw
    ) {
        if (DiscordUtil.getJda() == null) return;
        DiscordUtil.queueMessage(
                textChannel,
                DiscordSRV.translateMessage(
                        new MessageFormat(
                                "",
                                actionMessage,
                                "",
                                DiscordSRV.getAvatarUrl(player),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                null,
                                colorRaw,
                                null,
                                false,
                                DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl(),
                                DiscordSRV.getPlugin().getMainGuild() != null
                                        ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName()
                                        : DiscordUtil.getJda().getSelfUser().getName()
                        ),
                        (content, needsEscape) -> PlaceholderUtil.replacePlaceholdersToDiscord(content, player)
                ),
                true
        );
    }

    public enum Chat {GLOBAL, LOCAL}

    public enum RolePlayActionType {DO, IT, ME, TODO}

    public static class Colors {
        public static final TextColor
                CHAT_COLOR_PRIMARY = TextColor.color(171, 164, 148),
                CHAT_COLOR_SECONDARY = TextColor.color(241, 240, 227),
                JOIN_MESSAGE_COLOR_PRIMARY = TextColor.color(255, 238, 147),
                JOIN_MESSAGE_COLOR_SECONDARY = TextColor.color(252, 245, 199),
                RP_MESSAGE_MESSAGE_COLOR_PRIMARY = TextColor.color(255, 170, 0),
                RP_MESSAGE_MESSAGE_COLOR_SECONDARY = TextColor.color(255, 195, 105);
    }
}
