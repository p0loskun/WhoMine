package com.minersstudios.wholib.paper.custom.anomaly.task;

import com.minersstudios.wholib.paper.PaperConfig;
import com.minersstudios.wholib.paper.custom.anomaly.AnomalyBoundingBox;
import com.minersstudios.wholib.paper.custom.anomaly.action.SpawnParticlesAction;
import com.minersstudios.wholib.paper.WhoMine;
import com.minersstudios.wholib.paper.custom.anomaly.Anomaly;
import com.minersstudios.wholib.paper.custom.anomaly.AnomalyAction;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Main anomaly action task. This task is used to check if the player is in the
 * anomaly zone. When a player is in the anomaly zone, the action will be
 * performed. Otherwise, the action will be removed.
 * <br>
 * The task is registered in {@link PaperConfig#reload()} with
 * {@link PaperConfig#getAnomalyCheckRate()}.
 *
 * @see AnomalyAction
 * @see AnomalyBoundingBox
 */
public final class MainAnomalyActionTask implements Runnable {
    private final WhoMine plugin;
    private final Map<Player, Map<AnomalyAction, Long>> actionMap;
    private final Map<NamespacedKey, Anomaly> anomalyMap;

    public MainAnomalyActionTask(final @NotNull WhoMine plugin) {
        this.plugin = plugin;
        this.actionMap = plugin.getCache().getPlayerAnomalyActionMap();
        this.anomalyMap = plugin.getCache().getAnomalies();
    }

    @Override
    public void run() {
        final var onlinePlayers = this.plugin.getServer().getOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            return;
        }

        for (final var player : onlinePlayers) {
            for (final var anomaly : this.anomalyMap.values()) {
                final double radiusInside = anomaly.getBoundingBox().getRadiusInside(player);

                if (radiusInside == -1.0d) {
                    continue;
                }

                var timedAction = this.actionMap.get(player);
                final var ignorablePlayers = anomaly.getIgnorablePlayers();

                for (final var action : anomaly.getAnomalyActionMap().get(radiusInside)) {
                    if (
                            timedAction == null
                            || !timedAction.containsKey(action)
                    ) {
                        final boolean isIgnorable = ignorablePlayers.contains(player);

                        if (
                                isIgnorable
                                && action instanceof SpawnParticlesAction
                        ) {
                            action.putAction(player);
                            return;
                        } else if (!isIgnorable) {
                            timedAction = action.putAction(player);
                        }
                    }
                }

                if (timedAction == null) {
                    return;
                }

                final var ignorableItems = anomaly.getIgnorableItems();

                for (final var action : timedAction.keySet()) {
                    if (anomaly.isAnomalyActionRadius(action, radiusInside)) {
                        if (!(action instanceof SpawnParticlesAction)) {
                            action.doAction(player, ignorableItems);
                        }
                    } else {
                        action.removeAction(player);
                    }
                }

                return;
            }

            this.actionMap.remove(player);
        }
    }
}
