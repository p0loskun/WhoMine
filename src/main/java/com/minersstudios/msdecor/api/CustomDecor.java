package com.minersstudios.msdecor.api;

import com.minersstudios.mscore.util.LocationUtils;
import com.minersstudios.mscore.util.MSDecorUtils;
import com.minersstudios.msdecor.event.CustomDecorBreakEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public final class CustomDecor {
    private final CustomDecorData<?> data;
    private final ItemDisplay display;
    private final Interaction[] interactions;
    private final BoundingBox boundingBox;

    public CustomDecor(
            final @NotNull CustomDecorData<?> data,
            final @NotNull ItemDisplay display,
            final Interaction @NotNull [] interactions,
            final @NotNull BoundingBox boundingBox
    ) {
        this.data = data;
        this.display = display;
        this.interactions = interactions;
        this.boundingBox = boundingBox;
    }

    public static @NotNull Optional<CustomDecor> fromBlock(final @Nullable org.bukkit.block.Block block) {
        return block == null
                ? Optional.empty()
                : fromInteraction(MSDecorUtils.getNearbyInteraction(block.getLocation().toCenterLocation()));
    }

    public static @NotNull Optional<CustomDecor> fromInteraction(final @Nullable Interaction interaction) {
        if (interaction == null) return Optional.empty();

        final PersistentDataContainer container = interaction.getPersistentDataContainer();
        return container.isEmpty()
                ? Optional.empty()
                : DecorHitBox.isHitBoxParent(interaction)
                ? Optional.ofNullable(fromParent(interaction))
                : DecorHitBox.isHitBoxChild(interaction)
                ? Optional.ofNullable(fromChild(interaction))
                : Optional.empty();
    }

    public @NotNull CustomDecorData<?> getData() {
        return this.data;
    }

    public @NotNull ItemDisplay getDisplay() {
        return this.display;
    }

    public Interaction @NotNull [] getInteractions() {
        return this.interactions.clone();
    }

    public @NotNull BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public void destroy(
            final @NotNull Entity breaker,
            final boolean dropItem
    ) {
        final CustomDecorBreakEvent event = new CustomDecorBreakEvent(this, breaker);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;

        final CraftWorld world = (CraftWorld) breaker.getWorld();

        if (dropItem) {
            final ItemStack displayItemStack = this.display.getItemStack();
            assert displayItemStack != null;
            final ItemStack itemStack =
                    !this.data.isAnyTyped() || this.data.isDropsType()
                    ? displayItemStack
                    : this.data.getItem();
            final ItemMeta itemMeta = itemStack.getItemMeta();

            itemMeta.displayName(displayItemStack.getItemMeta().displayName());
            itemStack.setItemMeta(itemMeta);

            world.dropItemNaturally(
                    LocationUtils.nmsToBukkit(this.boundingBox.getCenter()),
                    itemStack
            );
        }

        if (!this.data.getHitBox().getType().isNone()) {
            CustomDecorDataImpl.fillBlocks(
                    breaker.getName(),
                    world.getHandle(),
                    LocationUtils.getBlockPosesBetween(
                            this.boundingBox.minX(),
                            this.boundingBox.minY(),
                            this.boundingBox.minZ(),
                            this.boundingBox.maxX(),
                            this.boundingBox.maxY(),
                            this.boundingBox.maxZ()
                    ),
                    Blocks.AIR
            );
        }

        for (final var interaction : this.interactions) {
            interaction.remove();
        }

        this.display.remove();
        this.data.getSoundGroup().playBreakSound(LocationUtils.nmsToBukkit(this.boundingBox.getCenter(), world));
    }

    public static void place(
            final @NotNull CustomDecorType type,
            final @NotNull Location blockLocation,
            final @NotNull Player player,
            final @NotNull BlockFace blockFace
    ) {
        place(type, blockLocation, player, blockFace, null, null);
    }

    public static void place(
            final @NotNull CustomDecorType type,
            final @NotNull Location blockLocation,
            final @NotNull Player player,
            final @NotNull BlockFace blockFace,
            final @Nullable EquipmentSlot hand
    ) {
        place(type, blockLocation, player, blockFace, hand, null);
    }

    public static void place(
            final @NotNull CustomDecorType type,
            final @NotNull Location blockLocation,
            final @NotNull Player player,
            final @NotNull BlockFace blockFace,
            final @Nullable EquipmentSlot hand,
            final @Nullable Component customName
    ) {
        type.getCustomDecorData().place(
                blockLocation,
                player,
                blockFace,
                hand,
                customName
        );
    }

    public static void destroyInBlock(
            final @NotNull Player player,
            final @NotNull Block block
    ) {
        destroyInBlock(player, block, player.getGameMode() == GameMode.SURVIVAL);
    }

    public static void destroyInBlock(
            final @NotNull Entity destroyer,
            final @NotNull Block block,
            final boolean dropItem
    ) {
        for (final var interaction : MSDecorUtils.getNearbyInteractions(block.getLocation().toCenterLocation())) {
            destroy(destroyer, interaction, dropItem);
        }
    }

    public static void destroy(
            final @NotNull Player player,
            final @NotNull Interaction interacted
    ) {
        destroy(player, interacted, player.getGameMode() == GameMode.SURVIVAL);
    }

    public static void destroy(
            final @NotNull Entity destroyer,
            final @NotNull Interaction interacted,
            final boolean dropItem
    ) {
        fromInteraction(interacted)
        .ifPresent(customDecor -> customDecor.destroy(destroyer, dropItem));
    }

    private static @Nullable CustomDecor fromParent(final @NotNull Interaction interaction) {
        final PersistentDataContainer container = interaction.getPersistentDataContainer();

        if (container.isEmpty()) return null;

        CustomDecorData<?> data = null;
        ItemDisplay display = null;
        final var interactions = new ArrayList<Interaction>();
        BoundingBox boundingBox = null;

        interactions.add(interaction);

        for (final var key : container.getKeys()) {
            final String value = container.get(key, PersistentDataType.STRING);

            if (StringUtils.isBlank(value)) continue;

            switch (key.getKey()) {
                case CustomDecorType.TYPE_TAG_NAME -> data = CustomDecorData.fromKey(value).orElse(null);
                case DecorHitBox.HITBOX_DISPLAY -> {
                    try {
                        if (interaction.getWorld().getEntity(UUID.fromString(value)) instanceof final ItemDisplay itemDisplay) {
                            display = itemDisplay;
                        }
                    } catch (final IllegalArgumentException ignored) {
                        return null;
                    }
                }
                case DecorHitBox.HITBOX_INTERACTIONS -> {
                    for (final var uuid : value.split(",")) {
                        try {
                            if (interaction.getWorld().getEntity(UUID.fromString(uuid)) instanceof final Interaction child) {
                                interactions.add(child);
                            }
                        } catch (final IllegalArgumentException ignored) {
                            return null;
                        }
                    }
                }
                case DecorHitBox.HITBOX_BOUNDING_BOX -> {
                    final String[] coordinates = value.split(",");

                    if (coordinates.length != 6) return null;

                    try {
                        boundingBox = new BoundingBox(
                                Integer.parseInt(coordinates[0]),
                                Integer.parseInt(coordinates[1]),
                                Integer.parseInt(coordinates[2]),
                                Integer.parseInt(coordinates[3]),
                                Integer.parseInt(coordinates[4]),
                                Integer.parseInt(coordinates[5])
                        );
                    } catch (final NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }

        return data == null
                || display == null
                || boundingBox == null
                ? null
                : new CustomDecor(
                        data,
                        display,
                        interactions.toArray(new Interaction[0]),
                        boundingBox
                );
    }

    private static @Nullable CustomDecor fromChild(final @NotNull Interaction interaction) {
        final String uuid =
                interaction.getPersistentDataContainer()
                .get(
                        DecorHitBox.HITBOX_CHILD_NAMESPACED_KEY,
                        PersistentDataType.STRING
                );

        try {
            return StringUtils.isBlank(uuid)
                    || !(interaction.getWorld().getEntity(UUID.fromString(uuid)) instanceof final Interaction parent)
                    ? null
                    : fromParent(parent);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }
}
