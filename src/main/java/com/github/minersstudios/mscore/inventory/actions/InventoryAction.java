package com.github.minersstudios.mscore.inventory.actions;

import com.github.minersstudios.mscore.inventory.CustomInventory;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for inventory actions
 * <br>
 * Used event types:
 * <ul>
 *     <li>{@link InventoryClickEvent}</li>
 *     <li>{@link InventoryCloseEvent}</li>
 *     <li>{@link InventoryOpenEvent}</li>
 * </ul>
 *
 * @param <E> Event type
 */
@FunctionalInterface
public interface InventoryAction<E extends Event> {

    /**
     * Performs action when event is triggered
     *
     * @param event           Event that triggered the action
     * @param customInventory Custom inventory that is involved in this event
     */
    void doAction(
            @NotNull E event,
            @NotNull CustomInventory customInventory
    );
}
