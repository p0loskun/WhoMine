package com.github.minersstudios.msessentials.player;

import com.github.minersstudios.mscore.utils.ChatUtils;
import com.github.minersstudios.msessentials.MSEssentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.util.InstantTypeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mute map with {@link UUID} and its {@link Params}.
 * All mutes stored in the "config/minersstudios/MSEssentials/muted_players.json" file.
 *
 * @see Params
 */
public class MuteMap {
    private final File file;
    private final Gson gson;
    private final Map<UUID, Params> map = new ConcurrentHashMap<>();

    /**
     * Mute map with {@link UUID} and its {@link Params}.
     * Loads mutes from the file.
     */
    public MuteMap() {
        this.file = new File(MSEssentials.getInstance().getPluginFolder(), "muted_players.json");
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .setPrettyPrinting()
                .create();
        this.reloadMutes();
    }

    /**
     * Gets mute parameters
     *
     * @param player Probably muted player
     * @return Creation and expiration date, reason and source of mute
     */
    public @Nullable Params getParams(@NotNull OfflinePlayer player) {
        return this.map.get(player.getUniqueId());
    }

    /**
     * @param player Probably muted player
     * @return True if the player is muted
     */
    @Contract("null -> false")
    public boolean isMuted(@Nullable OfflinePlayer player) {
        return player != null && this.map.containsKey(player.getUniqueId());
    }

    /**
     * Adds mute for the player
     *
     * @param player     Player who will be muted
     * @param expiration Date when the player will be unmuted
     * @param reason     Mute reason
     * @param source     Mute source, could be a player's nickname or CONSOLE
     */
    public void put(
            @NotNull OfflinePlayer player,
            @NotNull Instant expiration,
            @NotNull String reason,
            @NotNull String source
    ) {
        Instant created = Instant.now();
        UUID uuid = player.getUniqueId();

        this.map.put(uuid, Params.create(created, expiration, reason, source));
        this.saveFile();
    }

    /**
     * Removes mute from player
     *
     * @param player Muted player
     */
    public void remove(@Nullable OfflinePlayer player) {
        if (player == null) return;
        this.map.remove(player.getUniqueId());
        this.saveFile();
    }

    /**
     * @return The number of muted players
     */
    public int size() {
        return this.map.size();
    }

    /**
     * @return True if this map contains no muted players
     */
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    /**
     * @param uuid Player's UUID
     * @return True if this map contains a mute for the specified uuid
     */
    public boolean containsUniqueId(@Nullable UUID uuid) {
        return this.map.containsKey(uuid);
    }

    /**
     * @return An unmodifiable view of the UUIDs contained in this map
     */
    public @NotNull @UnmodifiableView Set<UUID> uuidSet() {
        return Set.copyOf(this.map.keySet());
    }

    /**
     * @return An unmodifiable view of the mappings contained in this map
     */
    public @NotNull @UnmodifiableView Set<Map.Entry<UUID, Params>> entrySet() {
        return Set.copyOf(this.map.entrySet());
    }

    /**
     * Reloads "muted_players.json" file
     */
    public void reloadMutes() {
        this.map.clear();

        if (!this.file.exists()) {
            this.createFile();
        } else {
            try {
                Type mapType = new TypeToken<Map<UUID, Params>>() {}.getType();
                String json = Files.readString(this.file.toPath(), StandardCharsets.UTF_8);
                Map<UUID, Params> jsonMap = this.gson.fromJson(json, mapType);

                if (jsonMap == null) {
                    this.createBackupFile();
                    this.reloadMutes();
                    return;
                }

                jsonMap.forEach((uuid, params) -> {
                    if (params != null && params.isValidate()) {
                        this.map.put(uuid, params);
                    } else {
                        ChatUtils.sendError("Failed to read the player params : " + uuid.toString() + " in \"muted_players.json\"");
                    }
                });
            } catch (Exception e) {
                this.createBackupFile();
                this.reloadMutes();
            }
        }
    }

    /**
     * Creates a new "muted_players.json" file
     */
    private void createFile() {
        try {
            if (this.file.createNewFile()) {
                this.saveFile();
            }
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to create a new \"muted_players.json\" file", e);
        }
    }

    /**
     * Creates a backup file of the "muted_players.json" file
     */
    private void createBackupFile() {
        File backupFile = new File(this.file.getParent(), this.file.getName() + ".OLD");
        Logger logger = Bukkit.getLogger();

        try {
            Files.move(this.file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.saveFile();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create \"muted_players.json.OLD\" backup file", e);
        }

        logger.log(Level.SEVERE, "Failed to read the \"muted_players.json\" file, creating a new file");
    }

    /**
     * Saves the mute map to the "muted_players.json" file
     */
    private void saveFile() {
        try (var writer = new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8)) {
            this.gson.toJson(this.map, writer);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save muted players", e);
        }
    }

    /**
     * Mute parameters, used in {@link MuteMap}
     * <br>
     * Parameters:
     * <ul>
     *     <li>created - date when the player was muted</li>
     *     <li>expiration - date when the player will be unmuted</li>
     *     <li>reason - mute reason</li>
     *     <li>source - mute source, could be a player's nickname or CONSOLE</li>
     * </ul>
     *
     * @see MuteMap
     */
    public static class Params {
        private final Instant created;
        private final Instant expiration;
        private final String reason;
        private final String source;

        private Params(
                Instant created,
                Instant expiration,
                String reason,
                String source
        ) {
            this.created = created;
            this.expiration = expiration;
            this.reason = reason;
            this.source = source;
        }

        /**
         * Creates a new {@link Params} with the specified parameters
         *
         * @param created    Date when the player was muted
         * @param expiration Date when the player will be unmuted
         * @param reason     Mute reason
         * @param source     Mute source, could be a player's nickname or CONSOLE
         * @return New {@link Params}
         */
        @Contract(value = "_, _, _, _ -> new")
        public static @NotNull Params create(
                @NotNull Instant created,
                @NotNull Instant expiration,
                @NotNull String reason,
                @NotNull String source
        ) {
            return new Params(created, expiration, reason, source);
        }

        /**
         * @return Date when the player was muted
         */
        public Instant getCreated() {
            return this.created;
        }

        /**
         * @return Date when the player will be unmuted
         */
        public Instant getExpiration() {
            return this.expiration;
        }

        /**
         * @return Mute reason
         */
        public String getReason() {
            return this.reason;
        }

        /**
         * @return Mute source, could be a player's nickname or CONSOLE
         */
        public String getSource() {
            return this.source;
        }

        /**
         * @return True if created, expiration, reason, source are not null
         */
        public boolean isValidate() {
            return this.created != null
                    && this.expiration != null
                    && this.reason != null
                    && this.source != null;
        }
    }
}
