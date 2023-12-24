package legacystore.store.legacysorteio.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PremioManager {

    private FileConfiguration config;
    private File cfile;

    public PremioManager(FileConfiguration config, File cfile) {
        this.config = config;
        this.cfile = cfile;
    }

    public void adicionarPremioItem(Player player, String nomePremio) {
        if (jogadorEstaSegurandoItem(player)) {
            ItemStack itemInHand = player.getInventory().getItemInHand();
            String material = itemInHand.getType().name();
            short data = itemInHand.getDurability();
            int quantidade = itemInHand.getAmount();
            ConfigurationSection premiosSection = config.getConfigurationSection("Premios");
            if (premiosSection == null) {
                premiosSection = config.createSection("Premios");
            }
            int proximoNumero = 1;
            while (premiosSection.contains(proximoNumero + "")) {
                proximoNumero++;
            }
            String path = "Premios." + proximoNumero;
            config.set(path + ".Tipo", "Item");
            int porcentagem = config.getInt(path + ".Porcentagem", 0);
            config.set(path + ".Porcentagem", porcentagem);
            config.set(path + ".Item.Material", material);
            config.set(path + ".Item.Data", data);
            config.set(path + ".Item.Quantidade", quantidade);
            config.set(path + ".Item.Nome", nomePremio);
            ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta != null && itemMeta.hasLore()) {
                config.set(path + ".Item.Lore", itemMeta.getLore());
            }
            salvarConfig();
        } else {
            player.sendMessage("§cVocê precisa segurar um item para adicionar como prêmio.");
        }
    }



    public boolean jogadorEstaSegurandoItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInHand();
        return itemInHand.getType() != Material.AIR;
    }

    public void adicionarPremioComando(String nomePremio, String comando) {
        ConfigurationSection premiosSection = config.getConfigurationSection("Premios");

        if (premiosSection == null) {
            premiosSection = config.createSection("Premios");
        }

        int proximoNumero = 1;

        while (premiosSection.contains(proximoNumero + "")) {
            proximoNumero++;
        }
        String path = "Premios." + proximoNumero;
        config.set(path + ".Tipo", "Comando");
        config.set(path + ".Comando.Nome", nomePremio);
        config.set(path + ".Comando.Comando", comando);
        salvarConfig();
    }

    public boolean isTipoItem(String premio) {
        ConfigurationSection premioSection = config.getConfigurationSection("Premios." + premio);
        return premioSection != null && "Item".equals(premioSection.getString("Tipo"));
    }

    public String escolherPremio() {
        int porcentagemTotal = calcularPorcentagemTotal();
        int sorteio = (int) (Math.random() * porcentagemTotal);
        int porcentagemAtual = 0;

        ConfigurationSection premiosSection = config.getConfigurationSection("Premios");
        if (premiosSection != null) {
            for (String premio : premiosSection.getKeys(false)) {
                int porcentagem = config.getInt("Premios." + premio + ".Porcentagem", 0);
                porcentagemAtual += porcentagem;

                if (sorteio < porcentagemAtual) {
                    return premio;
                }
            }
        }

        return null; // Retorna null se algo der errado
    }

    public void darPremio(Player player) {
        String premioEscolhido = escolherPremio();
        Bukkit.getConsoleSender().sendMessage("§a[Debug] Prêmio Escolhido: " + premioEscolhido);
        if (premioEscolhido != null) {
            if (isTipoItem(premioEscolhido)) {
                ItemStack premioItem = getItemStack(premioEscolhido);
                player.getInventory().addItem(premioItem);
            } else if (isTipoComando(premioEscolhido)) {
                String comando = getComando(premioEscolhido);
                // Lógica para executar o comando
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), comando);
            }
        }
    }

    public String getNomeItem(String premio) {
        return config.getString("Premios." + premio + ".Item.Nome", "§cNome do Prêmio Ausente");
    }

    public ItemStack getItemStack(String premio) {
        ConfigurationSection itemSection = config.getConfigurationSection("Premios." + premio + ".Item");
        if (itemSection != null) {
            int porcentagem = config.getInt("Premios." + premio + ".Porcentagem", 0);
            Material material = Material.getMaterial(itemSection.getString("Material"));
            int quantidade = itemSection.getInt("Quantidade", 1);
            short data = (short) itemSection.getInt("Data", 0);
            System.out.println("Porcentagem do prêmio " + premio + ": " + porcentagem);
            ItemStack item = new ItemStack(material, quantidade, data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && itemSection.isList("Lore")) {
                List<String> lore = itemSection.getStringList("Lore");
                lore = lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
                meta.setLore(lore);
            }
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemSection.getString("Nome")));
            item.setItemMeta(meta);

            return item;
        }
        return null;
    }

    public boolean isTipoComando(String premio) {
        ConfigurationSection premioSection = config.getConfigurationSection("Premios." + premio);
        return premioSection != null && "Comando".equals(premioSection.getString("Tipo"));
    }

    public String getNomeComando(String premio) {
        return config.getString("Premios." + premio + ".Comando.Nome", "§cNome do Prêmio Ausente");
    }

    public String getComando(String premio) {
        return config.getString("Premios." + premio + ".Comando.Comando", "");
    }

    public int calcularPorcentagemTotal() {
        int porcentagemTotal = 0;
        ConfigurationSection premiosSection = config.getConfigurationSection("Premios");

        if (premiosSection != null) {
            for (String premio : premiosSection.getKeys(false)) {
                porcentagemTotal += config.getInt("Premios." + premio + ".Porcentagem", 0);
            }
        }

        return porcentagemTotal;
    }

    private void salvarConfig() {
        try {
            config.save(cfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
