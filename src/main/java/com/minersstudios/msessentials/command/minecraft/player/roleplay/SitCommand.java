package com.minersstudios.msessentials.command.minecraft.player.roleplay;

import com.minersstudios.mscore.command.api.Command;
import com.minersstudios.mscore.command.api.CommandExecutor;
import com.minersstudios.mscore.language.LanguageRegistry;
import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.mscore.utility.ChatUtils;
import com.minersstudios.mscore.utility.Font;
import com.minersstudios.msessentials.MSEssentials;
import com.minersstudios.msessentials.player.PlayerInfo;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static net.kyori.adventure.text.Component.text;

@Command(
        command = "sit",
        aliases = {"s"},
        usage = " " + Font.Chars.RED_EXCLAMATION_MARK + " §cИспользуй: /<command> [речь]",
        description = "Сядь на картаны и порви жопу",
        playerOnly = true
)
public final class SitCommand extends CommandExecutor<MSEssentials> {
    private static final CommandNode<?> COMMAND_NODE =
            literal("sit")
            .then(argument("speech", StringArgumentType.greedyString()))
            .build();

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull org.bukkit.command.Command command,
            final @NotNull String label,
            final String @NotNull ... args
    ) {
        final Player player = (Player) sender;
        final PlayerInfo playerInfo = PlayerInfo.fromOnlinePlayer(this.getPlugin(), player);

        if (!player.getLocation().subtract(0.0d, 0.2d, 0.0d).getBlock().getType().isSolid()) {
            MSLogger.warning(
                    player,
                    LanguageRegistry.Components.COMMAND_SIT_IN_AIR
            );
            return true;
        }

        final String messageString = ChatUtils.extractMessage(args, 0);
        final Component message = messageString.isEmpty() ? null : text(messageString);

        if (
                message != null
                && playerInfo.isMuted()
        ) {
            MSLogger.warning(
                    player,
                    LanguageRegistry.Components.COMMAND_MUTE_ALREADY_RECEIVER
            );
            return true;
        }

        if (playerInfo.isSitting()) {
            playerInfo.unsetSitting(message);
        } else {
            playerInfo.setSitting(player.getLocation(), message);
        }
        return true;
    }

    @Override
    public @Nullable CommandNode<?> getCommandNode() {
        return COMMAND_NODE;
    }
}
