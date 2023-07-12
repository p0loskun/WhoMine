package com.github.minersstudios.msessentials;

import com.github.minersstudios.mscore.MSCore;
import com.github.minersstudios.mscore.utils.MSPluginUtils;
import com.github.minersstudios.msessentials.anomalies.Anomaly;
import com.github.minersstudios.msessentials.anomalies.tasks.MainAnomalyActionsTask;
import com.github.minersstudios.msessentials.anomalies.tasks.ParticleTask;
import com.github.minersstudios.msessentials.menu.CraftsMenu;
import com.github.minersstudios.msessentials.player.PlayerInfo;
import com.github.minersstudios.msessentials.player.ResourcePack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader class.
 * Use {@link MSEssentials#getConfiguration()} to get configuration instance.
 * Use {@link #reload()} to reload configuration and {@link #save(YamlConfiguration)} to save configuration.
 */
public final class Config {
    private final File file;

    public long anomalyCheckRate;
    public long anomalyParticlesCheckRate;
    public boolean developerMode;
    public String discordGlobalChannelId;
    public String discordLocalChannelId;
    public String version;
    public String user;
    public String repo;
    public String fullFileName;
    public String fullHash;
    public String liteFileName;
    public String liteHash;
    public double localChatRadius;
    public String mineSkinApiKey;

    public Config() {
        this.file = MSEssentials.getInstance().getConfigFile();
        this.reload();
    }

    /**
     * @return The config file, where the configuration is stored
     */
    public @NotNull File getFile() {
        return this.file;
    }

    /**
     * Reloads the config file
     */
    public void reload() {
        MSEssentials plugin = MSEssentials.getInstance();
        Logger logger = plugin.getLogger();
        File pluginFolder = plugin.getPluginFolder();
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(this.file);
        Cache cache = MSEssentials.getCache();

        this.developerMode = yamlConfig.getBoolean("developer-mode", false);
        this.anomalyCheckRate = yamlConfig.getLong("anomaly-check-rate", 100L);
        this.anomalyParticlesCheckRate = yamlConfig.getLong("anomaly-particles-check-rate", 10L);
        this.localChatRadius = yamlConfig.getDouble("chat.local.radius", 25);
        this.discordGlobalChannelId = yamlConfig.getString("chat.global.discord-channel-id");
        this.discordLocalChannelId = yamlConfig.getString("chat.local.discord-channel-id");
        this.version = yamlConfig.getString("resource-pack.version");
        this.user = yamlConfig.getString("resource-pack.user");
        this.repo = yamlConfig.getString("resource-pack.repo");
        this.fullFileName = yamlConfig.getString("resource-pack.full.file-name");
        this.fullHash = yamlConfig.getString("resource-pack.full.hash");
        this.liteFileName = yamlConfig.getString("resource-pack.lite.file-name");
        this.liteHash = yamlConfig.getString("resource-pack.lite.hash");
        this.mineSkinApiKey = yamlConfig.getString("skin.mine-skin-api-key", "");

        if (!cache.bukkitTasks.isEmpty()) {
            cache.bukkitTasks.forEach(BukkitTask::cancel);
        }

        cache.bukkitTasks.clear();
        cache.playerAnomalyActionMap.clear();
        cache.anomalies.clear();

        plugin.saveResource("anomalies/example.yml", true);
        File consoleDataFile = new File(pluginFolder, "players/console.yml");
        if (!consoleDataFile.exists()) {
            plugin.saveResource("players/console.yml", false);
        }

        cache.consolePlayerInfo = new PlayerInfo(UUID.randomUUID(), "$Console");

        plugin.runTaskAsync(ResourcePack::init);

        plugin.runTaskAsync(() -> {
            try (var path = Files.walk(Paths.get(pluginFolder + "/anomalies"))) {
                path
                .filter(file -> {
                    String fileName = file.getFileName().toString();
                    return Files.isRegularFile(file)
                            && !fileName.equals("example.yml")
                            && fileName.endsWith(".yml");
                })
                .map(Path::toFile)
                .forEach(file -> {
                    Anomaly anomaly = Anomaly.fromConfig(file);
                    cache.anomalies.put(anomaly.getNamespacedKey(), anomaly);
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "An error occurred while loading anomalies!", e);
            }
        });

        cache.bukkitTasks.add(plugin.runTaskTimer(
                new MainAnomalyActionsTask(),
                0L,
                this.anomalyCheckRate
        ));

        cache.bukkitTasks.add(plugin.runTaskTimer(
                new ParticleTask(),
                0L,
                this.anomalyParticlesCheckRate
        ));

        com.github.minersstudios.mscore.Cache msCoreCache = MSCore.getCache();

        var customBlockRecipes = msCoreCache.customBlockRecipes;
        var customDecorRecipes = msCoreCache.customDecorRecipes;
        var customItemRecipes = msCoreCache.customItemRecipes;

        plugin.runTaskTimer(task -> {
            if (
                    MSPluginUtils.isLoadedCustoms()
                    && !customBlockRecipes.isEmpty()
                    && !customDecorRecipes.isEmpty()
                    && !customItemRecipes.isEmpty()
            ) {
                CraftsMenu.putCrafts(CraftsMenu.Type.BLOCKS, customBlockRecipes);
                CraftsMenu.putCrafts(CraftsMenu.Type.DECORS, customDecorRecipes);
                CraftsMenu.putCrafts(CraftsMenu.Type.ITEMS, customItemRecipes);
                task.cancel();
            }
        }, 0L, 10L);

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    /**
     * Saves the configuration to the config file
     *
     * @param configuration The configuration to save
     */
    public void save(@NotNull YamlConfiguration configuration) {
        try {
            configuration.save(MSEssentials.getInstance().getConfigFile());
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving the config!", e);
        }
    }
}
