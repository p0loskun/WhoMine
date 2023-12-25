package com.minersstudios.msessentials.listener.impl.discord;

import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.mscore.utility.Font;
import com.minersstudios.msessentials.MSEssentials;
import com.minersstudios.msessentials.discord.BotHandler;
import com.minersstudios.msessentials.listener.api.discord.AbstractDiscordListener;
import com.minersstudios.msessentials.listener.api.discord.DiscordListener;
import com.minersstudios.msessentials.utility.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

@DiscordListener
public final class MessageReceivedListener extends AbstractDiscordListener {

    @Override
    public void onMessageReceived(final @NotNull MessageReceivedEvent event) {
        final User user = event.getAuthor();
        final Message message = event.getMessage();

        if (
                user.isBot()
                || user.isSystem()
        ) {
            return;
        }

        if (
                event.isFromGuild()
                && event.getChannel() == this.getPlugin().getCache().getDiscordManager().getGlobalChannel().orElse(null)
        ) {
            final Message referencedMessage = message.getReferencedMessage();
            final String reply =
                    referencedMessage != null
                    ? replaceReplyPlaceholders(referencedMessage)
                    : "";
            final Component messageComponent = craftComponent(message, reply);

            MessageUtils.sendGlobalMessage(messageComponent);
            MSLogger.info(null, messageComponent);
        } else if (event.isFromType(ChannelType.PRIVATE)) {
            final MSEssentials plugin = this.getPlugin();
            final long userID = user.getIdLong();
            final var handlerMap = plugin.getCache().getBotHandlers();
            BotHandler handler = handlerMap.get(userID);

            if (handler == null) {
                handler = new BotHandler(plugin, event);

                handlerMap.put(userID, handler);
            }

            handler.handleMessage(message);
        }
    }

    private static @NotNull Component craftComponent(
            final @NotNull Message message,
            final @NotNull String reply
    ) {
        return Font.Components.DISCORD
                .color(NamedTextColor.WHITE)
                .append(text(message.getAuthor().getName(), TextColor.color(112, 125, 223)))
                .append(text(reply, TextColor.color(152, 162, 249)))
                .append(text(" : ", TextColor.color(112, 125, 223)))
                .append(text(craftAttachmentString(message), TextColor.color(165, 165, 255)))
                .append(text(message.getContentDisplay(), TextColor.color(202, 202, 255)));
    }

    private static @NotNull String replaceReplyPlaceholders(final @NotNull Message repliedMessage) {
        return " (отвечая на \"" + craftAttachmentString(repliedMessage) + repliedMessage.getContentDisplay() + "\")";
    }

    private static @NotNull String craftAttachmentString(final @NotNull Message message) {
        return message.getAttachments().isEmpty()
                ? ""
                : message.getAttachments().size() > 1
                ? "(вложения) "
                : "(вложение) ";
    }
}
