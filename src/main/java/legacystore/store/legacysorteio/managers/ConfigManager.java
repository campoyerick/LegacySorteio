package legacystore.store.legacysorteio.managers;

import legacystore.store.legacysorteio.LegacySorteio;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final LegacySorteio plugin;
    private FileConfiguration config;
    File cfile;
    private final String configFileName = "config.yml";

    public ConfigManager(LegacySorteio plugin) {
        this.plugin = plugin;
        this.config = loadConfig();
    }

    public FileConfiguration loadConfig() {
        File configFile = new File(plugin.getDataFolder(), configFileName);
        if (!configFile.exists()) {
            plugin.saveResource(configFileName, false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        replaceAmpersand();

        return config;
    }

    public void reloadGlobalConfig() {
        File configFile = new File(plugin.getDataFolder(), configFileName);

        if (!configFile.exists()) {
            plugin.saveResource(configFileName, false);
        }

        try {
            config.load(configFile);
            replaceAmpersand();
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }


    private void replaceAmpersand() {
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                String value = config.getString(key);
                if (value != null) {
                    config.set(key, replaceAmpersand(value));
                }
            }
        }
        saveConfig(); // Adicione esta linha
    }

    private String replaceAmpersand(String input) {
        return input.replace("\\&", "&").replace("&", "§");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), configFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
