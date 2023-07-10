package com.github.minersstudios.mscore;

import com.github.minersstudios.mscore.command.Commodore;
import com.github.minersstudios.mscore.command.MSCommand;
import com.github.minersstudios.mscore.command.MSCommandExecutor;
import com.github.minersstudios.mscore.listener.MSListener;
import com.github.minersstudios.mscore.utils.ChatUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Java plugin and its main class.
 * It contains methods for auto registering commands and listeners.
 * This is an indirect implementation of {@link JavaPlugin}.
 *
 * @see #load()
 * @see #enable()
 * @see #disable()
 */
@SuppressWarnings("EmptyMethod")
public abstract class MSPlugin extends JavaPlugin {
    private final File pluginFolder = new File("config/minersstudios/" + this.getName() + "/");
    private final File configFile = new File(this.pluginFolder, "config.yml");
    private final Set<String> classNames = new HashSet<>();
    private final Map<MSCommand, MSCommandExecutor> msCommands = new HashMap<>();
    private final Set<Listener> listeners = new HashSet<>();
    private Commodore commodore;
    private FileConfiguration newConfig;
    private boolean loadedCustoms;

    private static final Field DATA_FOLDER_FIELD;
    private static final Constructor<PluginCommand> COMMAND_CONSTRUCTOR;

    static {
        try {
            DATA_FOLDER_FIELD = JavaPlugin.class.getDeclaredField("dataFolder");
            COMMAND_CONSTRUCTOR = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);

            DATA_FOLDER_FIELD.setAccessible(true);
            COMMAND_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find data folder field", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find command constructor", e);
        }
    }

    /**
     * Called after a plugin is loaded but before it has been enabled.
     * When multiple plugins are loaded, the onLoad() for all plugins is
     * called before any onEnable() is called.
     * Sets the plugin folder to "/config/minersstudios/PLUGIN_NAME".
     * Also loads the config from the new folder, class names, command instances and listener instances.
     * After that, it calls the load() method.
     *
     * @see #loadClassNames()
     * @see #loadCommands()
     * @see #loadListeners()
     */
    @Override
    public final void onLoad() {
        this.loadClassNames();
        this.loadCommands();
        this.loadListeners();

        try {
            DATA_FOLDER_FIELD.set(this, this.pluginFolder);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not set data folder", e);
        }

        this.load();
    }

    /**
     * Called when this plugin is enabled.
     * Registers all commands and listeners.
     * After that, it calls the enable() method.
     * Also logs the time it took to enable the plugin.
     *
     * @see #registerCommands()
     * @see #registerListeners()
     */
    @Override
    public final void onEnable() {
        long time = System.currentTimeMillis();
        this.commodore = new Commodore(this);

        this.registerCommands();
        this.registerListeners();

        this.enable();

        ChatUtils.sendFine("[" + this.getName() + "] Enabled in " + (System.currentTimeMillis() - time) + "ms");
    }

    /**
     * Called when this plugin is disabled.
     * After that, it calls the disable() method.
     * Also logs the time it took to disable the plugin.
     */
    @Override
    public final void onDisable() {
        long time = System.currentTimeMillis();

        this.disable();

        ChatUtils.sendFine("[" + this.getName() + "] Disabled in " + (System.currentTimeMillis() - time) + "ms");
    }

    /**
     * Same as {@link JavaPlugin#onLoad()}
     *
     * @see MSPlugin#onLoad()
     */
    public void load() {}

    /**
     * Same as {@link JavaPlugin#onEnable()}
     *
     * @see MSPlugin#onEnable()
     */
    public void enable() {}

    /**
     * Same as {@link JavaPlugin#onDisable()}
     *
     * @see MSPlugin#onDisable()
     */
    public void disable() {}

    /**
     * Gets the names of all plugin classes, similar to the package string
     * <br>
     * Example: "com.example.Example"
     *
     * @return The unmodifiable set of class names
     */
    public final @NotNull @Unmodifiable Set<String> getClassNames() {
        return Set.copyOf(this.classNames);
    }

    /**
     * @return The unmodifiable set of listeners
     */
    public final @NotNull @Unmodifiable Set<Listener> getListeners() {
        return Set.copyOf(this.listeners);
    }

    /**
     * @return The unmodifiable map of commands
     */
    public final @NotNull @Unmodifiable Map<MSCommand, MSCommandExecutor> getCommands() {
        return Map.copyOf(this.msCommands);
    }

    /**
     * @return Commodore instance
     */
    public final @NotNull Commodore getCommodore() {
        return this.commodore;
    }

    /**
     * @return Plugin config file "/config/minersstudios/PLUGIN_NAME/config.yml"
     */
    public final @NotNull File getConfigFile() {
        return this.configFile;
    }

    /**
     * @return Plugin folder "/config/minersstudios/PLUGIN_NAME"
     */
    public final @NotNull File getPluginFolder() {
        return this.pluginFolder;
    }

    /**
     * @param loadedCustoms True if the plugin has loaded the customs to the cache
     */
    public final void setLoadedCustoms(boolean loadedCustoms) {
        this.loadedCustoms = loadedCustoms;
    }

    /**
     * Used in :
     * <ul>
     *     <li>msBlock({@link Cache#customBlockMap})</li>
     *     <li>msDecor({@link Cache#customDecorMap})</li>
     *     <li>msItem({@link Cache#customItemMap})</li>
     * </ul>
     *
     * @return True if the plugin has loaded the customs to the cache
     */
    public final boolean isLoadedCustoms() {
        return this.loadedCustoms;
    }

    /**
     * Gets a {@link FileConfiguration} for this plugin, read through
     * "config.yml"
     * <br>
     * If there is a default config.yml embedded in this plugin, it will be
     * provided as a default for this Configuration.
     *
     * @return Plugin configuration
     */
    @Override
    public @NotNull FileConfiguration getConfig() {
        if (this.newConfig == null) {
            this.reloadConfig();
        }
        return this.newConfig;
    }

    /**
     * Discards any data in {@link #getConfig()} and reloads from disk.
     */
    @Override
    public void reloadConfig() {
        this.newConfig = YamlConfiguration.loadConfiguration(this.configFile);
        InputStream defaultInput = this.getResource("config.yml");

        if (defaultInput == null) return;

        InputStreamReader inputReader = new InputStreamReader(defaultInput, Charsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(inputReader);

        this.newConfig.setDefaults(configuration);
    }

    /**
     * Saves the {@link FileConfiguration} retrievable by {@link #getConfig()}
     */
    @Override
    public void saveConfig() {
        try {
            this.getConfig().save(this.configFile);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }
    }

    /**
     * Saves the raw contents of any resource embedded with a plugin's .jar
     * file assuming it can be found using {@link #getResource(String)}.
     * <br>
     * The resource is saved into the plugin's data folder using the same
     * hierarchy as the .jar file (subdirectories are preserved).
     *
     * @param resourcePath The embedded resource path to look for within the
     *                     plugin's .jar file. (No preceding slash).
     * @param replace If true, the embedded resource will overwrite the
     *                contents of an existing file.
     * @throws IllegalArgumentException if the resource path is null, empty,
     *                                  or points to a nonexistent resource.
     */
    @Override
    public void saveResource(
            @NotNull String resourcePath,
            boolean replace
    ) throws IllegalArgumentException {
        Preconditions.checkArgument(!resourcePath.isEmpty(), "ResourcePath cannot be empty");

        String path = resourcePath.replace('\\', '/');
        InputStream in = this.getResource(path);

        Preconditions.checkNotNull(in, "The embedded resource '" + path + "' cannot be found");

        Logger logger = this.getLogger();
        String dirPath = path.substring(0, Math.max(path.lastIndexOf('/'), 0));
        File outFile = new File(this.pluginFolder, path);
        File outDir = new File(this.pluginFolder, dirPath);
        String outFileName = outFile.getName();
        String outDirName = outDir.getName();

        if (!outDir.exists() && !outDir.mkdirs()) {
            logger.log(Level.WARNING, "Directory " + outDirName + " creation failed");
        }

        if (!outFile.exists() || replace) {
            try (
                    var out = new FileOutputStream(outFile);
                    in
            ) {
                byte[] buffer = new byte[1024];
                int read;

                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException ex) {
                this.getLogger().log(Level.SEVERE, "Could not save " + outFileName + " to " + outFile, ex);
            }
        } else {
            this.getLogger().log(Level.WARNING, "Could not save " + outFileName + " to " + outFile + " because " + outFileName + " already exists.");
        }
    }

    /**
     * Saves the raw contents of the default config.yml file to the location
     * retrievable by {@link #getConfig()}.
     * <br>
     * This should fail silently if the config.yml already exists.
     */
    @Override
    public void saveDefaultConfig() {
        if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
        }
    }

    /**
     * Loads all commands annotated with {@link MSCommand} the project.
     * All commands must be implemented using {@link MSCommandExecutor}.
     *
     * @see MSCommand
     */
    private void loadCommands() {
        Logger logger = this.getLogger();

        this.classNames.stream().parallel().forEach(className -> {
            try {
                Class<?> clazz = this.getClassLoader().loadClass(className);
                MSCommand msCommand = clazz.getAnnotation(MSCommand.class);

                if (msCommand != null) {
                    if (clazz.getDeclaredConstructor().newInstance() instanceof MSCommandExecutor msCommandExecutor) {
                        this.msCommands.put(msCommand, msCommandExecutor);
                    } else {
                        logger.log(Level.WARNING, "Loaded command that is not instance of MSCommandExecutor (" + className + ")");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load command", e);
            }
        });
    }

    /**
     * Registers all command in the project that is annotated with {@link MSCommand}.
     * All commands must be implemented using {@link MSCommandExecutor}.
     *
     * @see MSCommand
     */
    public void registerCommands() {
        this.msCommands.forEach(this::registerCommand);
    }

    /**
     * Loads all listeners annotated with {@link MSListener} the project.
     * All listeners must be implemented using {@link Listener}.
     *
     * @see MSListener
     */
    private void loadListeners() {
        Logger logger = this.getLogger();

        this.classNames.stream().parallel().forEach(className -> {
            try {
                Class<?> clazz = this.getClassLoader().loadClass(className);

                if (clazz.isAnnotationPresent(MSListener.class)) {
                    if (clazz.getDeclaredConstructor().newInstance() instanceof Listener listener) {
                        this.listeners.add(listener);
                    } else {
                        logger.log(Level.WARNING, "Registered listener that is not instance of Listener (" + className + ")");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load listener", e);
            }
        });
    }

    /**
     * Registers all listeners in the project that is annotated with {@link MSListener}.
     * All listeners must be implemented using {@link Listener}.
     *
     * @see MSListener
     */
    public void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        this.listeners.forEach(listener -> pluginManager.registerEvents(listener, this));
    }

    /**
     * @param msCommand Command to be registered
     * @param executor  Command executor
     */
    public final void registerCommand(
            @NotNull MSCommand msCommand,
            @NotNull MSCommandExecutor executor
    ) throws IllegalArgumentException {
        String name = msCommand.command();
        CommandNode<?> commandNode = executor.getCommandNode();
        PluginCommand bukkitCommand = this.getCommand(name);
        PluginCommand pluginCommand = bukkitCommand == null ? createCommand(name) : bukkitCommand;

        if (pluginCommand == null) {
            this.getLogger().log(Level.SEVERE, "Failed to register command : " + name);
            return;
        }

        List<String> aliases = Arrays.asList(msCommand.aliases());
        String usage = msCommand.usage();
        String description = msCommand.description();
        String permissionStr = msCommand.permission();

        if (!aliases.isEmpty()) {
            pluginCommand.setAliases(aliases);
        }

        if (!usage.isEmpty()) {
            pluginCommand.setUsage(usage);
        }

        if (!description.isEmpty()) {
            pluginCommand.setDescription(description);
        }

        if (!permissionStr.isEmpty()) {
            PluginManager pluginManager = this.getServer().getPluginManager();
            var children = new HashMap<String, Boolean>();
            String[] keys = msCommand.permissionParentKeys();
            boolean[] values = msCommand.permissionParentValues();

            if (keys.length != values.length) {
                this.getLogger().severe("Permission and boolean array lengths do not match in command : " + name);
            } else {
                for (int i = 0; i < keys.length; i++) {
                    children.put(keys[i], values[i]);
                }
            }

            if (pluginManager.getPermission(permissionStr) == null) {
                Permission permission = new Permission(permissionStr, msCommand.permissionDefault(), children);
                pluginManager.addPermission(permission);
            }

            pluginCommand.setPermission(permissionStr);
        }

        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);

        if (commandNode != null) {
            this.commodore.register(pluginCommand, (LiteralCommandNode<?>) commandNode, pluginCommand::testPermissionSilent);
        }

        this.getServer().getCommandMap().register(this.getName(), pluginCommand);
    }

    /**
     * Creates a new {@link PluginCommand} instance
     *
     * @param command Command name
     * @return A new {@link PluginCommand} instance or null if failed
     */
    public @Nullable PluginCommand createCommand(@NotNull String command) {
        try {
            return COMMAND_CONSTRUCTOR.newInstance(command, this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            this.getLogger().log(Level.WARNING, "Failed to create command : " + command, e);
            return null;
        }
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will run asynchronously.
     *
     * @param task The task to be run
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTaskAsync(@NotNull Runnable task) {
        return this.getServer().getScheduler().runTaskAsynchronously(this, task);
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param task   The task to be run
     * @param delay  The ticks to wait before running the task for the first time
     * @param period The ticks to wait between runs
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTaskTimerAsync(
            @NotNull Runnable task,
            long delay,
            long period
    ) {
        return this.getServer().getScheduler().runTaskTimerAsynchronously(this, task, delay, period);
    }

    /**
     * Returns a task that will run on the next server tick
     *
     * @param task The task to be run
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTask(@NotNull Runnable task) {
        return this.getServer().getScheduler().runTask(this, task);
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will run asynchronously after the specified number
     * of server ticks.
     *
     * @param task  The task to be run
     * @param delay The ticks to wait before running the task
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTaskLaterAsync(
            @NotNull Runnable task,
            long delay
    ) {
        return this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    /**
     * Returns a task that will run after the specified number of server ticks
     *
     * @param task  The task to be run
     * @param delay The ticks to wait before running the task
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTaskLater(
            @NotNull Runnable task,
            long delay
    ) {
        return this.getServer().getScheduler().runTaskLater(this, task, delay);
    }

    /**
     * Returns a task that will repeatedly run until cancelled, starting after
     * the specified number of server ticks
     *
     * @param task   The task to be run
     * @param delay  The ticks to wait before running the task
     * @param period The ticks to wait between runs
     * @return A BukkitTask that contains the id number
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public @NotNull BukkitTask runTaskTimer(
            @NotNull Runnable task,
            long delay,
            long period
    ) {
        return this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will run asynchronously.
     *
     * @param task The task to be run
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTaskAsync(@NotNull Consumer<BukkitTask> task) {
        this.getServer().getScheduler().runTaskAsynchronously(this, task);
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param task   The task to be run
     * @param delay  The ticks to wait before running the task for the first time
     * @param period The ticks to wait between runs
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTaskTimerAsync(
            @NotNull Consumer<BukkitTask> task,
            long delay,
            long period
    ) {
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, task, delay, period);
    }

    /**
     * Returns a task that will run on the next server tick
     *
     * @param task The task to be run
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTask(@NotNull Consumer<BukkitTask> task) {
        this.getServer().getScheduler().runTask(this, task);
    }

    /**
     * Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.
     * <br>
     * Returns a task that will run asynchronously after the specified number
     * of server ticks.
     *
     * @param task  The task to be run
     * @param delay The ticks to wait before running the task
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTaskLaterAsync(
            @NotNull Consumer<BukkitTask> task,
            long delay
    ) {
        this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    /**
     * Returns a task that will run after the specified number of server ticks
     *
     * @param task  The task to be run
     * @param delay The ticks to wait before running the task
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTaskLater(
            @NotNull Consumer<BukkitTask> task,
            long delay
    ) {
        this.getServer().getScheduler().runTaskLater(this, task, delay);
    }

    /**
     * Returns a task that will repeatedly run until cancelled, starting after
     * the specified number of server ticks
     *
     * @param task   The task to be run
     * @param delay  The ticks to wait before running the task
     * @param period The ticks to wait between runs
     * @throws IllegalArgumentException If current plugin is not enabled
     */
    public void runTaskTimer(
            @NotNull Consumer<BukkitTask> task,
            long delay,
            long period
    ) {
        this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
    }

    /**
     * Gathers the names of all plugin classes and converts them to the same string as the package
     * <br>
     * "com/example/Example.class" -> "com.example.Example"
     */
    private void loadClassNames() {
        try (var jarFile = new JarFile(this.getFile())) {
            this.classNames.addAll(
                    jarFile.stream().parallel()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.replace("/", ".").replace(".class", ""))
                    .toList()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
