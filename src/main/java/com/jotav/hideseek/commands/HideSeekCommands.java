package com.jotav.hideseek.commands;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.chat.ChatManager;
import com.jotav.hideseek.config.GameConfig;
import com.jotav.hideseek.game.GameManager;
import com.jotav.hideseek.stats.StatsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos principais do Hide and Seek
 */
public class HideSeekCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hns")
            // Comandos para todos os jogadores
            .then(Commands.literal("join")
                .executes(HideSeekCommands::joinGame))
            .then(Commands.literal("leave")
                .executes(HideSeekCommands::leaveGame))
            .then(Commands.literal("leaveall")
                .requires(source -> source.hasPermission(2))
                .executes(HideSeekCommands::leaveAllPlayers))
            
            // Comandos para OPs
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .executes(HideSeekCommands::startGame))
            .then(Commands.literal("stop")
                .requires(source -> source.hasPermission(2))
                .executes(HideSeekCommands::stopGame))
            .then(Commands.literal("lobbykick")
                .requires(source -> source.hasPermission(2))
                .executes(HideSeekCommands::lobbyKick))
            
            // Comandos de configuração
            .then(Commands.literal("set")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("lobby")
                    .executes(HideSeekCommands::setLobby))
                .then(Commands.literal("seekerspawn")
                    .executes(HideSeekCommands::setSeekerSpawn))
                .then(Commands.literal("mapboundary")
                    .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                            .executes(HideSeekCommands::setMapBoundary))))
                .then(Commands.literal("time")
                    .then(Commands.argument("phase", StringArgumentType.word())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 600))
                            .executes(HideSeekCommands::setTime)))))
            
            // Comando de randomização
            .then(Commands.literal("randomize")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("min_hiders", IntegerArgumentType.integer(1))
                    .then(Commands.argument("max_hiders", IntegerArgumentType.integer(1))
                        .executes(HideSeekCommands::randomizeTeams))))
            
            // Verificação de configuração
            .then(Commands.literal("checkconfig")
                .requires(source -> source.hasPermission(2))
                .executes(HideSeekCommands::checkConfig))
            
            // Comandos de UI
            .then(Commands.literal("scoreboard")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("show")
                    .executes(HideSeekCommands::showScoreboard))
                .then(Commands.literal("hide")
                    .executes(HideSeekCommands::hideScoreboard)))
            
            // Comandos de Estatísticas
            .then(Commands.literal("stats")
                .executes(HideSeekCommands::showOwnStats) // Suas próprias stats
                .then(Commands.argument("player", StringArgumentType.string())
                    .executes(HideSeekCommands::showPlayerStats))) // Stats de outro jogador
            .then(Commands.literal("leaderboard")
                .executes(HideSeekCommands::showDefaultLeaderboard) // Leaderboard padrão (vitórias)
                .then(Commands.argument("category", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // Sugestões para categorias de leaderboard
                        builder.suggest("wins");
                        builder.suggest("winrate");
                        builder.suggest("hider");
                        builder.suggest("seeker");  
                        builder.suggest("captures");
                        builder.suggest("survival");
                        builder.suggest("streak");
                        builder.suggest("games");
                        return builder.buildFuture();
                    })
                    .executes(HideSeekCommands::showCategoryLeaderboard)))
            .then(Commands.literal("globalstats")
                .executes(HideSeekCommands::showGlobalStats))
        );
    }
    
    private static int joinGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GameManager gameManager = GameManager.getInstance();
        
        if (gameManager.joinGame(player)) {
            context.getSource().sendSuccess(() -> Component.literal("Você entrou na fila do Hide and Seek!"), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Não foi possível entrar no jogo. O jogo pode estar em andamento."));
            return 0;
        }
    }
    
    private static int leaveGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GameManager gameManager = GameManager.getInstance();
        
        if (gameManager.leaveGame(player)) {
            context.getSource().sendSuccess(() -> Component.literal("Você saiu do Hide and Seek."), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Você não estava no jogo."));
            return 0;
        }
    }
    
    private static int startGame(CommandContext<CommandSourceStack> context) {
        GameManager gameManager = GameManager.getInstance();
        ChatManager chatManager = ChatManager.getInstance();
        
        if (gameManager.startGame()) {
            context.getSource().sendSuccess(() -> Component.literal("Jogo iniciado!"), true);
            return 1;
        } else {
            // Verificar e enviar mensagem específica de erro
            if (gameManager.getCurrentState() != com.jotav.hideseek.game.GameState.LOBBY) {
                context.getSource().sendFailure(Component.literal("Não é possível iniciar: jogo já em andamento."));
            } else {
                int current = gameManager.getPlayerManager().getLobbyCount();
                int required = Config.MIN_PLAYERS.get();
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    chatManager.notEnoughPlayers(player, current, required);
                }
                context.getSource().sendFailure(Component.literal("Jogadores insuficientes: " + current + "/" + required));
            }
            return 0;
        }
    }
    
    private static int stopGame(CommandContext<CommandSourceStack> context) {
        GameManager gameManager = GameManager.getInstance();
        gameManager.stopGame();
        
        context.getSource().sendSuccess(() -> Component.literal("Jogo parado e resetado."), true);
        return 1;
    }
    
    private static int lobbyKick(CommandContext<CommandSourceStack> context) {
        // Mesmo que stop - reset completo
        return stopGame(context);
    }
    
    private static int setLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = player.blockPosition();
        
        GameManager gameManager = GameManager.getInstance();
        gameManager.setLobbySpawn(pos, player.level().dimension());
        
        context.getSource().sendSuccess(() -> Component.literal(
            String.format("Lobby definido em: %d, %d, %d", pos.getX(), pos.getY(), pos.getZ())), false);
        return 1;
    }
    
    private static int setSeekerSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = player.blockPosition();
        
        GameManager gameManager = GameManager.getInstance();
        gameManager.setSeekerSpawn(pos, player.level().dimension());
        
        context.getSource().sendSuccess(() -> Component.literal(
            String.format("Spawn dos Seekers definido em: %d, %d, %d", pos.getX(), pos.getY(), pos.getZ())), false);
        return 1;
    }
    
    private static int setMapBoundary(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");
        
        // Garantir que pos1 é menor que pos2
        BlockPos min = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        BlockPos max = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
        
        GameManager gameManager = GameManager.getInstance();
        gameManager.setMapBoundary(min, max);
        
        context.getSource().sendSuccess(() -> Component.literal(
            String.format("Limites do mapa definidos: (%d,%d,%d) a (%d,%d,%d)", 
                         min.getX(), min.getY(), min.getZ(),
                         max.getX(), max.getY(), max.getZ())), false);
        return 1;
    }
    
    private static int setTime(CommandContext<CommandSourceStack> context) {
        String phase = StringArgumentType.getString(context, "phase").toUpperCase();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        GameConfig gameConfig = GameConfig.getInstance();
        boolean success = false;
        String phaseName = "";
        
        switch (phase) {
            case "HIDE", "HIDING", "ESCONDER" -> {
                success = gameConfig.setHideTime(seconds);
                phaseName = "esconder";
            }
            case "SEEK", "SEEKING", "BUSCAR" -> {
                success = gameConfig.setSeekTime(seconds);
                phaseName = "buscar";
            }
            case "START", "STARTING", "COUNTDOWN", "CONTAGEM" -> {
                success = gameConfig.setStartCountdown(seconds);
                phaseName = "contagem regressiva";
            }
            default -> {
                context.getSource().sendFailure(Component.literal(
                    "Fase inválida! Use: HIDE/ESCONDER, SEEK/BUSCAR, ou START/CONTAGEM"));
                return 0;
            }
        }
        
        if (success) {
            final String finalPhaseName = phaseName;
            final int finalSeconds = seconds;
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("✅ Tempo da fase %s definido para %d segundos", finalPhaseName, finalSeconds)), false);
            return 1;
        } else {
            final String finalPhaseName2 = phaseName;
            context.getSource().sendFailure(Component.literal(
                String.format("❌ Tempo inválido para fase %s. Consulte os limites permitidos.", finalPhaseName2)));
            return 0;
        }
    }
    
    private static int randomizeTeams(CommandContext<CommandSourceStack> context) {
        int minHiders = IntegerArgumentType.getInteger(context, "min_hiders");
        int maxHiders = IntegerArgumentType.getInteger(context, "max_hiders");
        
        if (minHiders > maxHiders) {
            context.getSource().sendFailure(Component.literal("Mínimo de Hiders não pode ser maior que o máximo."));
            return 0;
        }
        
        // TODO: Aplicar randomização imediatamente ou salvar configuração
        context.getSource().sendSuccess(() -> Component.literal(
            String.format("Randomização configurada: %d-%d Hiders", minHiders, maxHiders)), false);
        return 1;
    }
    
    private static int checkConfig(CommandContext<CommandSourceStack> context) {
        GameConfig gameConfig = GameConfig.getInstance();
        
        context.getSource().sendSuccess(() -> Component.literal("=== Configuração do Hide and Seek ==="), false);
        
        // Mostrar configurações atuais
        String allConfigs = gameConfig.getAllConfigurations();
        for (String line : allConfigs.split("\n")) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        
        // Verificar configurações faltando
        String missing = gameConfig.getMissingConfigurations();
        if (!missing.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("\n⚠️ Configurações faltando:"), false);
            for (String line : missing.split("\n")) {
                if (!line.trim().isEmpty()) {
                    context.getSource().sendSuccess(() -> Component.literal(line), false);
                }
            }
        } else {
            context.getSource().sendSuccess(() -> Component.literal("\n✅ Todas as configurações estão definidas!"), false);
        }
        
        return 1;
    }
    
    private static int showScoreboard(CommandContext<CommandSourceStack> context) {
        com.jotav.hideseek.ui.ScoreboardManager.getInstance().showScoreboard();
        context.getSource().sendSuccess(() -> Component.literal("Scoreboard exibido para todos os jogadores"), true);
        return 1;
    }
    
    private static int hideScoreboard(CommandContext<CommandSourceStack> context) {
        com.jotav.hideseek.ui.ScoreboardManager.getInstance().hideScoreboard();
        context.getSource().sendSuccess(() -> Component.literal("Scoreboard ocultado para todos os jogadores"), true);
        return 1;
    }
    
    private static int leaveAllPlayers(CommandContext<CommandSourceStack> context) {
        GameManager gameManager = GameManager.getInstance();
        
        // Remover todos os jogadores
        int removedPlayers = gameManager.removeAllPlayers();
        
        if (removedPlayers == 0) {
            context.getSource().sendFailure(Component.literal("Não há jogadores no jogo para remover."));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal(
            String.format("Todos os %d jogadores foram removidos do jogo e teleportados para o lobby.", removedPlayers)
        ), true);
        
        return 1;
    }
    
    // ================== COMANDOS DE ESTATÍSTICAS ==================
    
    /**
     * Mostra estatísticas próprias do jogador
     */
    private static int showOwnStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        StatsManager statsManager = StatsManager.getInstance();
        
        com.jotav.hideseek.stats.PlayerStats stats = statsManager.getPlayerStats(player);
        context.getSource().sendSuccess(() -> stats.getDetailedComponent(), false);
        
        return 1;
    }
    
    /**
     * Mostra estatísticas de outro jogador
     */
    private static int showPlayerStats(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        StatsManager statsManager = StatsManager.getInstance();
        
        com.jotav.hideseek.stats.PlayerStats stats = statsManager.findPlayerByName(playerName);
        if (stats == null) {
            context.getSource().sendFailure(Component.literal("Jogador '" + playerName + "' não encontrado nas estatísticas."));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> stats.getDetailedComponent(), false);
        return 1;
    }
    
    /**
     * Mostra leaderboard padrão (vitórias totais)
     */
    private static int showDefaultLeaderboard(CommandContext<CommandSourceStack> context) {
        StatsManager statsManager = StatsManager.getInstance();
        
        Component leaderboard = statsManager.getLeaderboardComponent(StatsManager.RankingType.GAMES_WON, 10);
        context.getSource().sendSuccess(() -> leaderboard, false);
        
        return 1;
    }
    
    /**
     * Mostra leaderboard por categoria específica
     */
    private static int showCategoryLeaderboard(CommandContext<CommandSourceStack> context) {
        String category = StringArgumentType.getString(context, "category").toLowerCase();
        StatsManager statsManager = StatsManager.getInstance();
        
        StatsManager.RankingType rankingType = switch (category) {
            case "wins" -> StatsManager.RankingType.GAMES_WON;
            case "winrate" -> StatsManager.RankingType.WIN_RATE;
            case "hider" -> StatsManager.RankingType.HIDER_WINS;
            case "seeker" -> StatsManager.RankingType.SEEKER_WINS;
            case "captures" -> StatsManager.RankingType.PLAYERS_CAPTURED;
            case "survival" -> StatsManager.RankingType.LONGEST_SURVIVAL;
            case "streak" -> StatsManager.RankingType.WIN_STREAK;
            case "games" -> StatsManager.RankingType.GAMES_PLAYED;
            default -> {
                context.getSource().sendFailure(Component.literal("Categoria inválida. Use: wins, winrate, hider, seeker, captures, survival, streak, games"));
                yield null;
            }
        };
        
        if (rankingType == null) {
            return 0;
        }
        
        Component leaderboard = statsManager.getLeaderboardComponent(rankingType, 10);
        context.getSource().sendSuccess(() -> leaderboard, false);
        
        return 1;
    }
    
    /**
     * Mostra estatísticas globais do servidor
     */
    private static int showGlobalStats(CommandContext<CommandSourceStack> context) {
        StatsManager statsManager = StatsManager.getInstance();
        
        Component globalStats = statsManager.getGlobalStatsComponent();
        context.getSource().sendSuccess(() -> globalStats, false);
        
        return 1;
    }
}