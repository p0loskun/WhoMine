package com.minersstudios.msdecor.command;

import com.minersstudios.mscore.plugin.MSLogger;
import com.minersstudios.msdecor.api.CustomDecorData;
import com.minersstudios.msessentials.MSEssentials;
import com.minersstudios.msessentials.player.PlayerInfo;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.minersstudios.mscore.language.LanguageRegistry.Components.*;
import static net.kyori.adventure.text.Component.text;

public final class GiveCommand {

    public static boolean runCommand(
            final @NotNull CommandSender sender,
            final String @NotNull ... args
    ) {
        if (args.length < 3) {
            return false;
        }

        final String playerArg = args[1];
        final String blockArg = args[2];
        final String amountArg = args.length == 4 ? args[3] : "1";
        final PlayerInfo playerInfo = PlayerInfo.fromString(MSEssentials.singleton(), playerArg);

        if (playerInfo == null) {
            MSLogger.severe(
                    sender,
                    ERROR_PLAYER_NOT_FOUND
            );
            return true;
        }

        final Player player = playerInfo.getOnlinePlayer();

        if (player == null) {
            MSLogger.warning(
                    sender,
                    ERROR_PLAYER_NOT_ONLINE
            );
            return true;
        }

        CustomDecorData.fromKey(blockArg).ifPresentOrElse(
                data -> {
                    final int amount;

                    try {
                        amount = Integer.parseInt(amountArg);
                    } catch (final NumberFormatException ignore) {
                        MSLogger.severe(
                                sender,
                                ERROR_WRONG_FORMAT
                        );
                        return;
                    }

                    final ItemStack itemStack = data.getItem();

                    itemStack.setAmount(amount);
                    player.getInventory().addItem(itemStack);
                    MSLogger.fine(
                            sender,
                            COMMAND_MSDECOR_GIVE_SUCCESS
                            .args(
                                    text(amount),
                                    itemStack.displayName(),
                                    playerInfo.getDefaultName()
                            )
                    );
                },
                () -> MSLogger.severe(
                        sender,
                        COMMAND_MSDECOR_GIVE_WRONG_DECOR
                )
        );

        return true;
    }
}
