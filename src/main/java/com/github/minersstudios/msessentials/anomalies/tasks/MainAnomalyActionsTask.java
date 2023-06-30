package com.github.minersstudios.msessentials.anomalies.tasks;

import com.github.minersstudios.msessentials.MSEssentials;
import com.github.minersstudios.msessentials.anomalies.Anomaly;
import com.github.minersstudios.msessentials.anomalies.AnomalyAction;
import com.github.minersstudios.msessentials.anomalies.actions.SpawnParticlesAction;
import com.github.minersstudios.msessentials.config.ConfigCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

public class MainAnomalyActionsTask implements Runnable {

    @Override
    public void run() {
        var onlinePlayers = Bukkit.getOnlinePlayers();

        if (onlinePlayers.isEmpty()) return;

        ConfigCache configCache = MSEssentials.getConfigCache();
        var playerActionMap = configCache.playerAnomalyActionMap;

        Bukkit.getScheduler().runTaskAsynchronously(
                MSEssentials.getInstance(),
                () -> onlinePlayers
                        .forEach(player -> {
                            for (var anomaly : configCache.anomalies.values()) {
                                Double radiusInside = anomaly.getBoundingBox().getRadiusInside(player);
                                boolean isIgnorable = anomaly.getIgnorablePlayers().contains(player);

                                if (radiusInside == null) continue;

                                var actionMap = playerActionMap.get(player);

                                for (var action : anomaly.getAnomalyActionMap().get(radiusInside)) {
                                    if (actionMap == null || !actionMap.containsKey(action)) {
                                        if (isIgnorable && action instanceof SpawnParticlesAction) {
                                            action.putAction(player);
                                            return;
                                        } else if (!isIgnorable) {
                                            actionMap = action.putAction(player);
                                        }
                                    }
                                }

                                if (actionMap == null) return;

                                for (var action : actionMap.keySet()) {
                                    if (anomaly.isAnomalyActionRadius(action, radiusInside)) {
                                        if (!(action instanceof SpawnParticlesAction)) {
                                            action.doAction(player, anomaly.getIgnorableItems());
                                        }
                                    } else {
                                        action.removeAction(player);
                                    }
                                }
                                return;
                            }
                            playerActionMap.remove(player);
                        })
        );
    }
}
