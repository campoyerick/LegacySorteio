package legacystore.store.legacysorteio.managers;

import legacystore.store.legacysorteio.LegacySorteio;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SorteioManager {
    private FileConfiguration config;
    private Connection connection;
    private boolean sorteioAtivo = false;
    private File cfile;

    private String jogadorSorteado = "";
    private List<String> ultimosGanhadores = new ArrayList<>();
    private boolean sorteioCancelado = false;

    public SorteioManager(FileConfiguration config) {
        this.config = config;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String host = config.getString("database.host");
        String port = config.getString("database.port");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            connection = DriverManager.getConnection(url, username, password);
            Bukkit.getConsoleSender().sendMessage("§bConexão com MYSQL estabelecida.");

            if (connection != null) {
                if (createTableIfNotExists()) {
                    Bukkit.getConsoleSender().sendMessage("§bDatabase e Tabela encontradas, conectado!");
                } else {
                    Bukkit.getConsoleSender().sendMessage("§cErro ao criar tabela no MySQL. Verifique suas configurações e certifique-se de que o MySQL está online.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§cErro ao conectar ao MySQL. Verifique suas configurações no arquivo config.yml e certifique-se de que o MySQL está online.");
        }
    }

    private boolean createTableIfNotExists() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ganhadores (nome VARCHAR(50), data TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void iniciarSorteio() {
        int minPlayers = config.getInt("Configuracoes.jogadoresminimos");

        if (Bukkit.getOnlinePlayers().size() < minPlayers && !sorteioCancelado) {
            String titulo = config.getString("Mensagens.JogadoresInsuficientes.Titulo");
            String subtitulo = config.getString("Mensagens.JogadoresInsuficientes.Subtitulo");
            enviarTituloParaTodos(titulo, subtitulo);
            return;
        }

        sorteioAtivo = true;
        String titulo = config.getString("Mensagens.IniciandoSorteio.Titulo");
        String subtitulo = config.getString("Mensagens.IniciandoSorteio.Subtitulo");
        enviarTituloParaTodos(titulo, subtitulo);
        Bukkit.getScheduler().runTaskLater(LegacySorteio.getPlugin(LegacySorteio.class), this::sortearJogador, 80L);
    }

    public void darPremioAoGanhador() {
        Bukkit.getConsoleSender().sendMessage("§a[Debug] Início do método darPremioAoGanhador");

        if (sorteioAtivo && jogadorSorteado != null && !jogadorSorteado.isEmpty()) {
            PremioManager premioManager = new PremioManager(config, cfile);
            premioManager.darPremio(Bukkit.getPlayer(jogadorSorteado));
            salvarGanhadorNoBanco(jogadorSorteado);
            adicionarGanhador(jogadorSorteado);
        }

        Bukkit.getConsoleSender().sendMessage("§a[Debug] Fim do método darPremioAoGanhador");
    }


    private void sortearJogador() {
        if (sorteioCancelado) {
            return;
        }

        Player[] jogadoresOnline = Bukkit.getOnlinePlayers().toArray(new Player[0]);

        if (jogadoresOnline.length > 0) {
            Player jogadorGanhador = jogadoresOnline[(int) (Math.random() * jogadoresOnline.length)];
            jogadorSorteado = jogadorGanhador.getName();
            ConfigurationSection premiosConfig = config.getConfigurationSection("Premios");
            Set<String> chavesPremios = premiosConfig.getKeys(false);

            // Certifique-se de que existem prêmios configurados
            if (chavesPremios != null && !chavesPremios.isEmpty()) {
                // Use o PremioManager para obter as informações do prêmio
                PremioManager premioManager = new PremioManager(config, cfile);
                String premioEscolhido = premioManager.escolherPremio();
                String premioNome;

                if (premioEscolhido != null) {
                    if (premioManager.isTipoItem(premioEscolhido)) {
                        premioNome = premioManager.getNomeItem(premioEscolhido);
                    } else {
                        premioNome = premioManager.getNomeComando(premioEscolhido);
                    }
                } else {
                    // Se não houver prêmios configurados, envie uma mensagem apropriada
                    String titulo = config.getString("Mesagens.Faltadejogador.Titulo");
                    String subtitulo = config.getString("Mesagens.Faltadejogador.Subtitulo");
                    enviarTituloParaTodos(titulo, subtitulo);
                    return; // Adicione esta linha para sair do método se não houver prêmios
                }

                // Salvar ganhador apenas se houver prêmio a ser dado
                if (!premioNome.isEmpty()) {
                    darPremioAoGanhador();

                    String titulo = config.getString("Mensagens.SortearJogador.Titulo")
                            .replace("{ganhador}", jogadorSorteado)
                            .replace("{premio}", premioNome);

                    String subtitulo = config.getString("Mensagens.SortearJogador.Subtitulo")
                            .replace("{ganhador}", jogadorSorteado)
                            .replace("{premio}", premioNome);

                    enviarTituloParaTodos(titulo, subtitulo);
                }
            } else {
                // Se não houver prêmios configurados, envie uma mensagem apropriada
                String titulo = config.getString("Mesagens.Faltadejogador.Titulo");
                String subtitulo = config.getString("Mesagens.Faltadejogador.Subtitulo");
                enviarTituloParaTodos(titulo, subtitulo);
            }
        } else {
            // Se não houver jogadores online, envie uma mensagem apropriada
            String titulo = config.getString("Mesagens.Faltadejogador.Titulo");
            String subtitulo = config.getString("Mesagens.Faltadejogador.Subtitulo");
            enviarTituloParaTodos(titulo, subtitulo);
        }

        sorteioAtivo = false;
    }


    private void darItemAoJogador(Player jogador, ItemStack item) {
        jogador.getInventory().addItem(item);
    }

    private void executarComando(String comando, Player jogador) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando.replace("{player}", jogador.getName()));
    }

    private void salvarGanhadorNoBanco(String ganhador) {
        if (connection == null) {
            Bukkit.getConsoleSender().sendMessage("§cTentativa de salvar ganhador no MySQL, mas a conexão não está disponível.");
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO ganhadores (nome) VALUES (?)")) {
            statement.setString(1, ganhador);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§cErro ao salvar ganhador no MySQL.");
        }
    }

    public void sorteioCancelado() {
        if (sorteioAtivo) {
            sorteioAtivo = false;
            sorteioCancelado = true;
            String titulo = config.getString("Mensagens.SorteioCancelado.Titulo");
            String subtitulo = config.getString("Mensagens.SorteioCancelado.Subtitulo");
            enviarTituloParaTodos(titulo, subtitulo);
        }
    }

    private void enviarTituloParaTodos(String titulo, String subtitulo) {
        for (Player jogador : Bukkit.getOnlinePlayers()) {
            jogador.sendTitle(titulo, subtitulo);
        }
    }

    public boolean isSorteioAtivo() {
        return sorteioAtivo;
    }

    public void reiniciarSorteio() {
        sorteioCancelado = false;
    }

    public void adicionarGanhador(String ganhador) {
        ultimosGanhadores.add(ganhador);
        if (ultimosGanhadores.size() > 10) {
            ultimosGanhadores.remove(0); // Manter apenas os últimos 10 ganhadores
        }
    }

    public void forcarInicioSorteio() {
        if (sorteioAtivo) {
            String titulo = config.getString("Mensagens.SorteioCancelado.Titulo");
            String subtitulo = config.getString("Mensagens.SorteioCancelado.Subtitulo");
            enviarTituloParaTodos(titulo, subtitulo);
        } else {
            sorteioAtivo = true;
            reiniciarSorteio();
            iniciarSorteio();
            String titulo = config.getString("Mensagens.IniciandoSorteio.Titulo");
            String subtitulo = config.getString("Mensagens.IniciandoSorteio.Subtitulo");
            enviarTituloParaTodos(titulo, subtitulo);
            Bukkit.getScheduler().runTaskLater(LegacySorteio.getPlugin(LegacySorteio.class), this::sortearJogador, 80L);
        }
    }

    public List<String> getUltimosGanhadores() {
        List<String> ultimosGanhadores = new ArrayList<>();

        if (connection == null) {
            Bukkit.getConsoleSender().sendMessage("§cTentativa de obter últimos ganhadores do MySQL, mas a conexão não está disponível.");
            return ultimosGanhadores;
        }

        Bukkit.getConsoleSender().sendMessage("§aBuscando últimos ganhadores...");

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT nome FROM ganhadores ORDER BY data DESC LIMIT 10")) {
            while (resultSet.next()) {
                ultimosGanhadores.add(resultSet.getString("nome"));
            }

            Bukkit.getConsoleSender().sendMessage("§bUltimos ganhadores: §f" + String.join(", ", ultimosGanhadores));
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§bErro ao obter últimos ganhadores do MySQL.");
        }

        return ultimosGanhadores;
    }

    public String getJogadorSorteado() {
        return jogadorSorteado;
    }
}