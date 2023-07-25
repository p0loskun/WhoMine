package com.minersstudios.msblock.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.function.Predicate;

public final class PlayerUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ImmutableSet<EntityType> MOB_FILTER = Sets.immutableEnumSet(
            //<editor-fold desc="Ignorable mob types">
            EntityType.DROPPED_ITEM,
            EntityType.ARROW,
            EntityType.SPECTRAL_ARROW,
            EntityType.AREA_EFFECT_CLOUD,
            EntityType.DRAGON_FIREBALL,
            EntityType.EGG,
            EntityType.FISHING_HOOK,
            EntityType.WITHER_SKULL,
            EntityType.TRIDENT,
            EntityType.SNOWBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.FIREBALL,
            EntityType.FIREWORK,
            EntityType.SPLASH_POTION,
            EntityType.THROWN_EXP_BOTTLE,
            EntityType.EXPERIENCE_ORB,
            EntityType.LLAMA_SPIT,
            EntityType.LIGHTNING
            //</editor-fold>
    );

    @Contract(value = " -> fail")
    private PlayerUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void openShulkerBoxWithoutAnimation(@NotNull Player player, @NotNull ShulkerBox shulkerBox) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        Container inventory = ((CraftInventory) shulkerBox.getInventory()).getInventory();

        if (inventory != null && !serverPlayer.isSpectator()) {
            int containerCounter = serverPlayer.nextContainerCounter();
            AbstractContainerMenu container = CraftEventFactory.callInventoryOpenEvent(serverPlayer, new ShulkerBoxMenu(containerCounter, serverPlayer.getInventory(), inventory), false);

            container.setTitle(((MenuProvider) inventory).getDisplayName());
            shulkerBox.getWorld().playSound(shulkerBox.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 0.5f, RANDOM.nextFloat() * 0.1f + 0.9f);

            serverPlayer.containerMenu = container;

            if (!serverPlayer.isImmobile()) {
                serverPlayer.connection.send(new ClientboundOpenScreenPacket(container.containerId, container.getType(), container.getTitle()));
            }

            serverPlayer.initMenu(container);
        }
    }


    /**
     * @param player player
     * @return target block
     */
    public static @Nullable Block getTargetBlock(@NotNull Player player) {
        Location eyeLocation = player.getEyeLocation();
        RayTraceResult rayTraceResult = player.getWorld().rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), 4.5d, FluidCollisionMode.NEVER, false);
        return rayTraceResult != null ? rayTraceResult.getHitBlock() : null;
    }

    /**
     * @param player      player
     * @param targetBlock target block
     * @return target entity
     */
    public static @Nullable Entity getTargetEntity(@NotNull Player player, @Nullable Block targetBlock) {
        Location eyeLocation = player.getEyeLocation();
        Predicate<Entity> filter = entity -> entity != player && !MOB_FILTER.contains(entity.getType());
        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(eyeLocation, eyeLocation.getDirection(), 4.5d, filter);
        if (rayTraceResult == null) return null;
        Entity targetEntity = rayTraceResult.getHitEntity();

        return targetBlock != null
                && targetEntity != null
                && eyeLocation.distance(targetBlock.getLocation()) <= eyeLocation.distance(targetEntity.getLocation())
                ? null
                : targetEntity;
    }
}
