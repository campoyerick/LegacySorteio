package legacystore.store.legacysorteio.commands;

import legacystore.store.legacysorteio.managers.ConfigManager;
import legacystore.store.legacysorteio.managers.PremioManager;
import legacystore.store.legacysorteio.managers.SorteioManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SorteioComando implements CommandExecutor {

    private SorteioManager sorteioManager;
    private final PremioManager premioManager;
    private final ConfigManager configManager;
    FileConfiguration config;
    File cfile;

    public SorteioComando(SorteioManager sorteioManager, ConfigManager configManager, FileConfiguration config, PremioManager premioManager) {
        this.sorteioManager = sorteioManager;
        this.configManager = configManager;
        this.config = config;
        this.premioManager = premioManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("iniciar")) {
                if (sender.hasPermission("legacystore.sorteioadmin")) {
                    if (!sorteioManager.isSorteioAtivo()) {
                        sorteioManager.reiniciarSorteio();
                        sorteioManager.iniciarSorteio();
                    } else {
                        sender.sendMessage("§cO sorteio já está em andamento.");
                    }
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            } else if (args[0].equalsIgnoreCase("cancelar")) {
                if (sender.hasPermission("legacystore.sorteioadmin")) {
                    sorteioManager.sorteioCancelado();
                    sender.sendMessage("§cSorteio cancelado.");
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            } else if (args[0].equalsIgnoreCase("listar")) {
                listarUltimosGanhadores(sender);
            } else if (args[0].equalsIgnoreCase("forceload")) {
                if (sender.hasPermission("legacystore.sorteioadmin")) {
                    sorteioManager.forcarInicioSorteio();
                    sender.sendMessage("§aSorteio iniciado mesmo sem o mínimo de jogadores!");
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("legacystore.sorteioadmin")) {
                    configManager.reloadGlobalConfig();
                    sender.sendMessage("§aConfigurações Recarregadas!");
                    return true;
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            } else if (args[0].equalsIgnoreCase("premios")) {
                if (sender.hasPermission("legacystore.sorteioadmin")) {
                    enviarMensagemRecompensas(sender);
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            } else if (args[0].equalsIgnoreCase("premio")) {
            if (args.length >= 4 && args[1].equalsIgnoreCase("adicionar")) {
                String tipoPremio = args[2];
                String nomePremio = args[3];

                if (tipoPremio.equalsIgnoreCase("item")) {
                    if (args.length == 4) {  // Verifica se não há argumentos adicionais
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            premioManager.adicionarPremioItem(player, nomePremio);
                            sender.sendMessage("§aPrêmio do tipo 'item' adicionado com sucesso!");
                        } else {
                            sender.sendMessage("§cApenas jogadores podem adicionar prêmios do tipo item.");
                        }
                    } else {
                        sender.sendMessage("§cUso incorreto. /sorteio premio adicionar item <nome>");
                    }
                } else if (tipoPremio.equalsIgnoreCase("comando")) {
                    if (args.length >= 5) {
                        String comando = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        premioManager.adicionarPremioComando(nomePremio, comando);
                        sender.sendMessage("§aPrêmio do tipo comando adicionado com sucesso!");
                    } else {
                        sender.sendMessage("§cUso incorreto. /sorteio premio adicionar comando <nome> <comando>");
                    }
                } else {
                    sender.sendMessage("§cTipo de prêmio inválido. Use 'item' ou 'comando'.");
                }
            } else {
                enviarMensagemRecompensas(sender);
            }
        } else {
            if (sender.hasPermission("legacystore.sorteioadmin")) {
                    enviarMensagensDeAjuda(sender);
                } else {
                    enviarMensagemSemPermissao(sender);
                }
            }
        } else {
            if (sender.hasPermission("legacystore.sorteioadmin")) {
                enviarMensagensDeAjuda(sender);
            } else {
                enviarMensagemSemPermissao(sender);
            }
        }
        return true;
    }

    private void listarUltimosGanhadores(CommandSender sender) {
        int quantidadeUltimosGanhadores = config.getInt("Configuracoes.quantidadeultimosganhadores", 5);
        List<String> ultimosGanhadores = sorteioManager.getUltimosGanhadores();
        sender.sendMessage(" §bUltimos ganhadores:");
        int contador = 1;
        for (String ganhador : ultimosGanhadores) {
            if (contador <= quantidadeUltimosGanhadores) {
                sender.sendMessage(" §b- §f" + ganhador);
                contador++;
            } else {
                break;
            }
        }
    }

    private void enviarMensagemRecompensas(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(" §b/sorteio premio adicionar <tipo> <nome> §8- §fAdicione um novo premio");
        sender.sendMessage("");
    }

    private void enviarMensagensDeAjuda(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(" §b/sorteio iniciar §8- §fUse para iniciar o sorteio.");
        sender.sendMessage(" §b/sorteio cancelar §8- §fUse para cancelar um sorteio em andamento.");
        sender.sendMessage(" §b/sorteio premios §8- §fVer como adicionar itens sem sair do game.");
        sender.sendMessage(" §b/sorteio listar §8- §fListe os últimos ganhadores.");
        sender.sendMessage(" §b/sorteio forceload §8- §fUse para iniciar sem o mínimo de jogadores.");
        sender.sendMessage(" §b/sorteio reload §8- §fUse para recarregar as configurações.");
        sender.sendMessage("");
    }

    private void enviarMensagemSemPermissao(CommandSender sender) {
        sender.sendMessage("§cVocê não tem permissão para executar este comando.");
    }
}
