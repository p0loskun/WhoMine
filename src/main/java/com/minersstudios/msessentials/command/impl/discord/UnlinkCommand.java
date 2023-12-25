package com.minersstudios.msessentials.command.impl.discord;

import com.minersstudios.mscore.language.LanguageFile;
import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.msessentials.command.api.discord.InteractionHandler;
import com.minersstudios.msessentials.command.api.discord.SlashCommand;
import com.minersstudios.msessentials.command.api.discord.SlashCommandExecutor;
import com.minersstudios.msessentials.discord.BotHandler;
import com.minersstudios.msessentials.player.PlayerInfo;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.minersstudios.mscore.language.LanguageRegistry.Components.COMMAND_DISCORD_UNLINK_DISCORD_SUCCESS;
import static com.minersstudios.mscore.language.LanguageRegistry.Components.COMMAND_DISCORD_UNLINK_MINECRAFT_SUCCESS;
import static net.kyori.adventure.text.Component.text;

@SlashCommand
public final class UnlinkCommand extends SlashCommandExecutor {

    public UnlinkCommand() {
        super(
                Commands.slash("unlink", "Unlink Discord account from Minecraft account")
        );
    }

    @Override
    public void onInteract(final @NotNull InteractionHandler handler) {
        handler.deferReply();

        final PlayerInfo playerInfo = handler.retrievePlayerInfo();

        if (playerInfo == null) {
            return;
        }

        playerInfo.unlinkDiscord();
        handler.send(
                BotHandler.craftEmbed(
                        LanguageFile.renderTranslation(
                                COMMAND_DISCORD_UNLINK_DISCORD_SUCCESS
                                .args(
                                        playerInfo.getDefaultName(),
                                        text(playerInfo.getPlayerFile().getPlayerName().getNickname())
                                )
                        )
                )
        );

        final Player onlinePlayer = playerInfo.getOnlinePlayer();

        if (onlinePlayer != null) {
            MSLogger.fine(
                    onlinePlayer,
                    COMMAND_DISCORD_UNLINK_MINECRAFT_SUCCESS
                            .args(text(handler.getInteraction().getUser().getName()))
            );
        }
    }
}
