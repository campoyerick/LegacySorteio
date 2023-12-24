package legacystore.store.legacysorteio;

import legacystore.store.legacysorteio.commands.SorteioComando;
import legacystore.store.legacysorteio.managers.ConfigManager;
import legacystore.store.legacysorteio.managers.PremioManager;
import legacystore.store.legacysorteio.managers.SorteioManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public final class LegacySorteio extends JavaPlugin {

    private SorteioManager sorteioManager;
    private PremioManager premioManager;
    private ConfigManager configManager;

    private final String githubRepoUrl = "https://api.github.com/repos/campoyerick/LegacySorteio/releases/latest";
    private String currentVersion;

    private BukkitTask updateTask;
    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("§b  _      ______ _____          _______     _______ _______ ____  _____  ______ ");
        Bukkit.getConsoleSender().sendMessage("§b | |    |  ____/ ____|   /\\   / ____\\ \\   / / ____|__   __/ __ \\|  __ \\|  ____|");
        Bukkit.getConsoleSender().sendMessage("§b | |    | |__ | |  __   /  \\ | |     \\ \\_/ / (___    | | | |  | | |__) | |__   ");
        Bukkit.getConsoleSender().sendMessage("§b | |    |  __|| | |_ | / /\\ \\| |      \\   / \\___ \\   | | | |  | |  _  /|  __|  ");
        Bukkit.getConsoleSender().sendMessage("§b | |____| |___| |__| |/ ____ \\ |____   | |  ____) |  | | | |__| | | \\ \\| |____ ");
        Bukkit.getConsoleSender().sendMessage("§b |______|______\\_____/_/    \\_\\_____|  |_| |_____/   |_|  \\____/|_|  \\_\\______|");
        Bukkit.getConsoleSender().sendMessage("§b                                                                               ");
        Bukkit.getConsoleSender().sendMessage("§b                                                                               ");
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        configManager.reloadGlobalConfig();
        premioManager = new PremioManager(getConfig(), new File(getDataFolder(), "config.yml"));
        sorteioManager = new SorteioManager(configManager.getConfig());
        getCommand("sorteio").setExecutor(new SorteioComando(sorteioManager, configManager, configManager.getConfig(), premioManager));
        int minutos = 15;
        long ticks = minutos * 60 * 20; //20 ticks por segundo

        new BukkitRunnable(){
            @Override
            public void run() {
                Bukkit.getConsoleSender().sendMessage("§bEstá apreciando o plugin? Explore outras opções incriveis adquirindo nossos plugins!");
                Bukkit.getConsoleSender().sendMessage("§bAdquira os melhores plugins em: §fhttps://discord.gg/WdBv6mpt5R §blegacystore.store");
            }
        }.runTaskTimer(this, 0, ticks);

        PluginDescriptionFile pluginDescription = getDescription();
        currentVersion = pluginDescription.getVersion();
        checkForUpdate(); // Verificar atualização imediatamente ao iniciar

        // Remova a tarefa periódica de verificação, pois não é mais necessária.
        // updateTask = new BukkitRunnable() {
        //     @Override
        //     public void run() {
        //         checkForUpdate();
        //     }
        // }.runTaskTimerAsynchronously(this, 0, 3 * 20);
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    private void checkForUpdate() {
        try (InputStream inputStream = new URL(githubRepoUrl).openStream(); Scanner scanner = new Scanner(inputStream)) {
            if (scanner.hasNext()) {
                String jsonData = scanner.useDelimiter("\\A").next();
                String[] parts = jsonData.split("\"tag_name\":\"");

                if (parts.length > 1) {
                    String latestVersion = parts[1].split("\",")[0];

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        Bukkit.getConsoleSender().sendMessage("§aUma nova versão do plugin está disponível! Versão atual: " + currentVersion + ", Versão mais recente: " + latestVersion);
                        Bukkit.getConsoleSender().sendMessage("§aVocê pode baixar a nova versão em: https://legacystore.store/legacysorteio.html");
                    }
                } else {
                    Bukkit.getConsoleSender().sendMessage("§cErro ao obter informações da versão. Resposta inesperada do servidor.");
                }
            }
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("§cErro ao verificar atualizações: " + e.getMessage());
        }
    }


}
