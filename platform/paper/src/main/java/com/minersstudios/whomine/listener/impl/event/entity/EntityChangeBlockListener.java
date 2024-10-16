package com.minersstudios.whomine.listener.impl.event.entity;

import com.minersstudios.whomine.api.event.handle.CancellableHandler;
import com.minersstudios.whomine.api.listener.ListenFor;
import com.minersstudios.whomine.event.PaperEventContainer;
import com.minersstudios.whomine.event.PaperEventListener;
import com.minersstudios.whomine.utility.MSDecorUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@ListenFor(EntityChangeBlockEvent.class)
public final class EntityChangeBlockListener extends PaperEventListener {

    @CancellableHandler
    public void onEntityChangeBlock(final @NotNull PaperEventContainer<EntityChangeBlockEvent> container) {
        final EntityChangeBlockEvent event = container.getEvent();
        final Block block = event.getBlock();

        if (
                event.getEntity() instanceof FallingBlock
                && MSDecorUtils.isCustomDecor(block)
        ) {
            event.setCancelled(true);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(event.getTo()));
        }
    }
}
