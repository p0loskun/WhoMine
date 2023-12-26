package com.minersstudios.msessentials.command.impl.minecraft.player;

import com.google.common.collect.ImmutableList;
import com.minersstudios.mscore.command.api.Command;
import com.minersstudios.mscore.command.api.AbstractCommandExecutor;
import com.minersstudios.mscore.language.LanguageFile;
import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.mscore.utility.Font;
import com.minersstudios.msessentials.MSEssentials;
import com.minersstudios.msessentials.discord.BotHandler;
import com.minersstudios.msessentials.discord.DiscordManager;
import com.minersstudios.msessentials.menu.DiscordLinkCodeMenu;
import com.minersstudios.msessentials.player.PlayerInfo;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.minersstudios.mscore.language.LanguageRegistry.Components.*;
import static com.minersstudios.mscore.utility.SharedConstants.DISCORD_LINK;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;

@Command(
        command = "discord",
        usage = " " + Font.Chars.RED_EXCLAMATION_MARK + " §cИспользуй: /<command> [параметры]",
        description = "Дискорд команды",
        permissionDefault = PermissionDefault.NOT_OP,
        playerOnly = true
)
public final class DiscordCommand extends AbstractCommandExecutor<MSEssentials> {
    private static final List<String> TAB = ImmutableList.of("link", "unlink");
    private static final CommandNode<?> COMMAND_NODE =
            literal("discord")
            .then(literal("link"))
            .then(literal("unlink"))
            .build();

    private static final TranslatableComponent DISCORD_MESSAGE =
            COMMAND_DISCORD
            .args(
                    text(DISCORD_LINK)
                    .hoverEvent(showText(LINK_HOVER.color(NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.openUrl(DISCORD_LINK)),
                    text("/discord link")
                    .hoverEvent(showText(COMMAND_HOVER_RUN.color(NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/discord link"))
            );

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull org.bukkit.command.Command command,
            final @NotNull String label,
            final String @NotNull ... args
    ) {
        final MSEssentials plugin = this.getPlugin();
        final Player player = (Player) sender;

        if (args.length > 0) {
            switch (args[0]) {
                case "link" -> plugin.openCustomInventory(DiscordLinkCodeMenu.class, player);
                case "unlink" -> {
                    final PlayerInfo playerInfo = PlayerInfo.fromOnlinePlayer(plugin, player);
                    final long id = playerInfo.unlinkDiscord();

                    if (id == -1L) {
                        MSLogger.warning(
                                sender,
                                COMMAND_DISCORD_UNLINK_NO_LINKS
                        );
                        return true;
                    }

                    final DiscordManager discordManager = plugin.getCache().getDiscordManager();

                    discordManager.retrieveUser(id)
                    .ifPresent(user -> {
                        discordManager.sendEmbeds(
                                user,
                                BotHandler.craftEmbed(
                                        LanguageFile.renderTranslation(
                                                COMMAND_DISCORD_UNLINK_DISCORD_SUCCESS
                                                .args(
                                                        playerInfo.getDefaultName(),
                                                        text(player.getName())
                                                )
                                        )
                                )
                        );
                        MSLogger.fine(
                                player,
                                COMMAND_DISCORD_UNLINK_MINECRAFT_SUCCESS
                                .args(text(user.getName()))
                        );
                    });
                }
                default -> {
                    return false;
                }
            }
        } else {
            MSLogger.warning(sender, DISCORD_MESSAGE);
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull org.bukkit.command.Command command,
            final @NotNull String label,
            final String @NotNull ... args
    ) {
        return args.length == 1 ? TAB : EMPTY_TAB;
    }

    @Override
    public @Nullable CommandNode<?> getCommandNode() {
        return COMMAND_NODE;
    }
}
