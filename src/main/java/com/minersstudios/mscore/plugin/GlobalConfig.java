package com.minersstudios.mscore.plugin;

import com.minersstudios.mscore.plugin.config.LanguageFile;
import com.minersstudios.mscore.plugin.config.MSConfig;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * Configuration loader class.
 * Use {@link MSPlugin#getGlobalConfig()} to get configuration instance.
 * Use {@link #reload()} to reload configuration and {@link #save()} to save configuration.
 */
public final class GlobalConfig extends MSConfig {
    public String languageCode;
    public String languageFolderLink;
    public DateTimeFormatter timeFormatter;

    /**
     * Global configuration constructor.
     * All variables must be initialized in {@link #reloadVariables()}
     */
    public GlobalConfig() {
        super(new File("config/minersstudios/config.yml"));
    }

    /**
     * Reloads config variables.
     * <br>
     * NOTE: Not updates the {@link LanguageFile}.
     * Use {@link LanguageFile#reloadLanguage()} to reload language file.
     */
    public void reloadVariables() {
        this.timeFormatter = DateTimeFormatter.ofPattern(this.yaml.getString("date-format", "EEE, yyyy-MM-dd HH:mm z"));
        this.languageCode = this.yaml.getString("language.code", "ru_ru");
        this.languageFolderLink = this.yaml.getString("language.folder-link", "https://github.com/MinersStudios/WMTranslations/raw/release/lang/");
    }

    /**
     * Reloads default config variables
     */
    public void reloadDefaultVariables() {
        this.setIfNotExists("date-format", "EEE, yyyy-MM-dd HH:mm z");
        this.setIfNotExists("language.code", "ru_ru");
        this.setIfNotExists("language.folder-link", "https://github.com/MinersStudios/WMTranslations/raw/release/lang/");
    }
}
