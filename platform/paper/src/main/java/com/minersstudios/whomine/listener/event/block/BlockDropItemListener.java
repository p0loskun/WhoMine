package com.minersstudios.whomine.listener.event.block;

import com.minersstudios.wholib.event.handle.CancellableHandler;
import com.minersstudios.wholib.listener.ListenFor;
import com.minersstudios.wholib.paper.custom.item.renameable.RenameableItemRegistry;
import com.minersstudios.wholib.paper.event.PaperEventContainer;
import com.minersstudios.wholib.paper.event.PaperEventListener;
import com.minersstudios.wholib.utility.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

@ListenFor(BlockDropItemEvent.class)
public final class BlockDropItemListener extends PaperEventListener {

    @CancellableHandler
    public void onBlockDropItem(final @NotNull PaperEventContainer<BlockDropItemEvent> container) {
        final BlockDropItemEvent event = container.getEvent();
        final var items = event.getItems();

        if (items.size() != 1) {
            return;
        }

        final Item entity = items.getFirst();
        final ItemStack item = entity.getItemStack();

        if (!Tag.SHULKER_BOXES.isTagged(item.getType())) {
            return;
        }

        final ItemMeta meta = item.getItemMeta();
        final Component displayName = meta.displayName();

        if (displayName == null) {
            return;
        }

        final String serialized = ChatUtils.serializePlainComponent(displayName);

        RenameableItemRegistry.fromRename(serialized, item)
        .ifPresent(renameableItem -> {
            final ItemStack renameableItemStack = renameableItem.craftRenamed(item, serialized);

            if (renameableItemStack != null) {
                entity.setItemStack(renameableItemStack);
            }
        });
    }
}
