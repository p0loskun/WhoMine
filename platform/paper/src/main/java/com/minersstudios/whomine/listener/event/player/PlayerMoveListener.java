package com.minersstudios.whomine.listener.event.player;

import com.minersstudios.wholib.paper.PaperCache;
import com.minersstudios.wholib.listener.ListenFor;
import com.minersstudios.wholib.paper.custom.block.CustomBlockData;
import com.minersstudios.wholib.paper.custom.block.CustomBlockRegistry;
import com.minersstudios.wholib.paper.event.PaperEventContainer;
import com.minersstudios.wholib.paper.event.PaperEventListener;
import com.minersstudios.wholib.paper.utility.BlockUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import com.minersstudios.wholib.event.handle.CancellableHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

@ListenFor(PlayerMoveEvent.class)
public final class PlayerMoveListener extends PaperEventListener {

    @CancellableHandler
    public void onPlayerMove(final @NotNull PaperEventContainer<PlayerMoveEvent> container) {
        final PlayerMoveEvent event = container.getEvent();
        final PaperCache cache = container.getModule().getCache();

        final Player player = event.getPlayer();
        final Block block = player.getLocation().subtract(0.0d, 0.15d, 0.0d).getBlock();

        if (cache.getWorldDark().isInWorldDark(event.getFrom())) {
            event.setCancelled(true);
        }

        if (
                player.getGameMode() != GameMode.SPECTATOR
                && !player.isFlying()
                && !player.isSneaking()
        ) {
            final double distance = event.getFrom().distance(event.getTo());

            if (
                    distance != 0.0d
                    && cache.getStepMap().addDistance(player, distance)
                    && BlockUtils.isWoodenSound(block.getType())
            ) {
                final Location stepLocation = block.getLocation().toCenterLocation();

                CustomBlockRegistry.fromBlockData(block.getBlockData())
                .orElse(CustomBlockData.defaultData())
                .getSoundGroup().playStepSound(stepLocation);
            }
        }
    }
}
