package com.jotav.hideseek.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jotav.hideseek.HideSeek;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gerenciador central de estatísticas do Hide and Seek
 */
public class StatsManager {
    private static StatsManager instance;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Path statsFile;
    private final Gson gson;
    
    // Tipos de ranking disponíveis
    public enum RankingType {
        GAMES_WON("Vitórias Totais", "gamesWon"),
        WIN_RATE("Taxa de Vitória", "winRate"),
        HIDER_WINS("Vitórias como Hider", "hiderWins"),
        SEEKER_WINS("Vitórias como Seeker", "seekerWins"),
        PLAYERS_CAPTURED("Capturas Feitas", "playersCaptured"),
        LONGEST_SURVIVAL("Maior Sobrevivência", "longestSurvival"),
        WIN_STREAK("Maior Sequência de Vitórias", "longestWinStreak"),
        GAMES_PLAYED("Jogos Jogados", "gamesPlayed");
        
        private final String displayName;
        private final String statField;
        
        RankingType(String displayName, String statField) {
            this.displayName = displayName;
            this.statField = statField;
        }
        
        public String getDisplayName() { return displayName; }
        public String getStatField() { return statField; }
    }
    
    private StatsManager() {
        // Definir caminho do arquivo de estatísticas
        this.statsFile = Paths.get("hideseek_stats.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Carregar estatísticas existentes
        loadStats();
    }
    
    public static StatsManager getInstance() {
        if (instance == null) {
            instance = new StatsManager();
        }
        return instance;
    }
    
    // ================== GERENCIAMENTO DE ESTATÍSTICAS ==================
    
    /**
     * Obtém estatísticas de um jogador (cria se não existir)
     */
    public PlayerStats getPlayerStats(ServerPlayer player) {
        return getPlayerStats(player.getUUID(), player.getName().getString());
    }
    
    /**
     * Obtém estatísticas por UUID (cria se não existir)
     */
    public PlayerStats getPlayerStats(UUID playerUuid, String playerName) {
        return playerStats.computeIfAbsent(playerUuid, uuid -> new PlayerStats(uuid, playerName));
    }
    
    /**
     * Obtém estatísticas por UUID (pode retornar null se não existir)
     */
    public PlayerStats getPlayerStatsReadOnly(UUID playerUuid) {
        return playerStats.get(playerUuid);
    }
    
    /**
     * Atualiza nome conhecido de um jogador
     */
    public void updatePlayerName(ServerPlayer player) {
        PlayerStats stats = getPlayerStats(player);
        stats.updateLastKnownName(player.getName().getString());
        saveStatsAsync(); // Salvar de forma assíncrona
    }
    
    /**
     * Remove estatísticas de um jogador (para limpeza/reset)
     */
    public boolean removePlayerStats(UUID playerUuid) {
        boolean removed = playerStats.remove(playerUuid) != null;
        if (removed) {
            saveStatsAsync();
        }
        return removed;
    }
    
    /**
     * Limpa todas as estatísticas
     */
    public void clearAllStats() {
        playerStats.clear();
        saveStatsAsync();
    }
    
    // ================== MÉTODOS PARA REGISTRAR EVENTOS ==================
    
    /**
     * Registra início de jogo para um jogador
     */
    public void recordGameStart(ServerPlayer player, boolean isHider) {
        PlayerStats stats = getPlayerStats(player);
        stats.startGame(isHider);
        HideSeek.LOGGER.debug("Recorded game start for {}: isHider={}", player.getName().getString(), isHider);
    }
    
    /**
     * Registra vitória para um jogador
     */
    public void recordWin(ServerPlayer player, boolean wasHider) {
        PlayerStats stats = getPlayerStats(player);
        stats.recordWin(wasHider);
        saveStatsAsync();
        HideSeek.LOGGER.info("Recorded win for {}: wasHider={}", player.getName().getString(), wasHider);
    }
    
    /**
     * Registra derrota para um jogador
     */
    public void recordLoss(ServerPlayer player, boolean wasHider) {
        PlayerStats stats = getPlayerStats(player);
        stats.recordLoss(wasHider);
        saveStatsAsync();
        HideSeek.LOGGER.info("Recorded loss for {}: wasHider={}", player.getName().getString(), wasHider);
    }
    
    /**
     * Registra que um jogador foi capturado
     */
    public void recordPlayerCaptured(ServerPlayer capturedPlayer) {
        PlayerStats stats = getPlayerStats(capturedPlayer);
        stats.recordCapture();
        saveStatsAsync();
        HideSeek.LOGGER.info("Recorded capture for {}", capturedPlayer.getName().getString());
    }
    
    /**
     * Registra que um jogador fez uma captura
     */
    public void recordPlayerMadeCapture(ServerPlayer seekerPlayer) {
        PlayerStats stats = getPlayerStats(seekerPlayer);
        stats.recordPlayerCaptured();
        saveStatsAsync();
        HideSeek.LOGGER.info("Recorded capture made by {}", seekerPlayer.getName().getString());
    }
    
    /**
     * Registra tempo de sobrevivência de um Hider
     */
    public void recordHidingTime(ServerPlayer hider, long seconds) {
        PlayerStats stats = getPlayerStats(hider);
        stats.addHidingTime(seconds);
        saveStatsAsync();
    }
    
    /**
     * Registra tempo de busca de um Seeker
     */
    public void recordSeekingTime(ServerPlayer seeker, long seconds) {
        PlayerStats stats = getPlayerStats(seeker);
        stats.addSeekingTime(seconds);
        saveStatsAsync();
    }
    
    /**
     * Registra tempo para primeira captura de um Seeker
     */
    public void recordCaptureTime(ServerPlayer seeker, long seconds) {
        PlayerStats stats = getPlayerStats(seeker);
        stats.recordCaptureTime(seconds);
        saveStatsAsync();
    }
    
    // ================== SISTEMA DE RANKINGS ==================
    
    /**
     * Gera ranking dos melhores jogadores por categoria
     */
    public List<Map.Entry<String, Double>> getLeaderboard(RankingType type, int limit) {
        return playerStats.values().stream()
            .filter(stats -> stats.getGamesPlayed() > 0) // Apenas jogadores que jogaram
            .map(stats -> {
                double value = getStatValue(stats, type);
                return new AbstractMap.SimpleEntry<>(stats.getLastKnownName(), value);
            })
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Ordem decrescente
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtém valor de estatística baseado no tipo de ranking
     */
    private double getStatValue(PlayerStats stats, RankingType type) {
        return switch (type) {
            case GAMES_WON -> stats.getGamesWon();
            case WIN_RATE -> stats.getGamesPlayed() >= 5 ? stats.getWinRate() : 0; // Mínimo 5 jogos para taxa de vitória
            case HIDER_WINS -> stats.getHiderWins();
            case SEEKER_WINS -> stats.getSeekerWins();
            case PLAYERS_CAPTURED -> stats.getPlayersCaptured();
            case LONGEST_SURVIVAL -> stats.getLongestSurvivalTimeSeconds();
            case WIN_STREAK -> stats.getLongestWinStreak();
            case GAMES_PLAYED -> stats.getGamesPlayed();
        };
    }
    
    /**
     * Gera componente de texto com leaderboard
     */
    public Component getLeaderboardComponent(RankingType type, int limit) {
        List<Map.Entry<String, Double>> ranking = getLeaderboard(type, limit);
        
        net.minecraft.network.chat.MutableComponent component = Component.literal("🏆 TOP " + limit + " - " + type.getDisplayName())
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("\n" + "=".repeat(30)).withStyle(ChatFormatting.GRAY));
        
        if (ranking.isEmpty()) {
            return component.append(Component.literal("\nNenhum jogador encontrado.").withStyle(ChatFormatting.GRAY));
        }
        
        for (int i = 0; i < ranking.size(); i++) {
            Map.Entry<String, Double> entry = ranking.get(i);
            String position = String.valueOf(i + 1);
            String playerName = entry.getKey();
            double value = entry.getValue();
            
            // Cor da posição
            ChatFormatting positionColor = switch (i) {
                case 0 -> ChatFormatting.GOLD;   // 1º lugar
                case 1 -> ChatFormatting.GRAY;   // 2º lugar  
                case 2 -> ChatFormatting.DARK_RED; // 3º lugar
                default -> ChatFormatting.WHITE;
            };
            
            // Formato do valor dependendo do tipo
            String formattedValue = formatStatValue(value, type);
            
            component.append(Component.literal("\n" + position + ". ").withStyle(positionColor, ChatFormatting.BOLD))
                .append(Component.literal(playerName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(formattedValue).withStyle(ChatFormatting.YELLOW));
        }
        
        return component;
    }
    
    /**
     * Formata valor de estatística para exibição
     */
    private String formatStatValue(double value, RankingType type) {
        return switch (type) {
            case WIN_RATE -> String.format("%.1f%%", value);
            case LONGEST_SURVIVAL -> PlayerStats.formatTime((long) value);
            default -> String.valueOf((long) value);
        };
    }
    
    // ================== PERSISTÊNCIA ==================
    
    /**
     * Salva estatísticas de forma assíncrona (não bloqueia)
     */
    private void saveStatsAsync() {
        // Em um ambiente real, você poderia usar um ThreadPoolExecutor
        // Por simplicidade, vamos salvar diretamente
        saveStats();
    }
    
    /**
     * Salva estatísticas no arquivo
     */
    public void saveStats() {
        try {
            Type mapType = new TypeToken<Map<UUID, PlayerStats>>(){}.getType();
            String json = gson.toJson(playerStats, mapType);
            
            try (FileWriter writer = new FileWriter(statsFile.toFile())) {
                writer.write(json);
            }
            
            HideSeek.LOGGER.debug("Saved stats for {} players", playerStats.size());
        } catch (IOException e) {
            HideSeek.LOGGER.error("Failed to save player stats", e);
        }
    }
    
    /**
     * Carrega estatísticas do arquivo
     */
    public void loadStats() {
        if (!Files.exists(statsFile)) {
            HideSeek.LOGGER.info("Stats file not found, starting with empty stats");
            return;
        }
        
        try {
            String json = Files.readString(statsFile);
            Type mapType = new TypeToken<Map<UUID, PlayerStats>>(){}.getType();
            Map<UUID, PlayerStats> loadedStats = gson.fromJson(json, mapType);
            
            if (loadedStats != null) {
                playerStats.clear();
                playerStats.putAll(loadedStats);
                HideSeek.LOGGER.info("Loaded stats for {} players", playerStats.size());
            }
        } catch (IOException e) {
            HideSeek.LOGGER.error("Failed to load player stats", e);
        } catch (Exception e) {
            HideSeek.LOGGER.error("Failed to parse player stats JSON", e);
        }
    }
    
    // ================== MÉTODOS UTILITÁRIOS ==================
    
    /**
     * Obtém número total de jogadores com estatísticas
     */
    public int getTotalPlayersCount() {
        return playerStats.size();
    }
    
    /**
     * Obtém estatísticas globais do servidor
     */
    public Component getGlobalStatsComponent() {
        int totalPlayers = playerStats.size();
        int totalGames = playerStats.values().stream().mapToInt(PlayerStats::getGamesPlayed).sum();
        int totalHiderWins = playerStats.values().stream().mapToInt(PlayerStats::getHiderWins).sum();
        int totalSeekerWins = playerStats.values().stream().mapToInt(PlayerStats::getSeekerWins).sum();
        
        return Component.literal("")
            .append(Component.literal("📊 Estatísticas Globais").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal("\n" + "=".repeat(20)).withStyle(ChatFormatting.GRAY))
            .append(Component.literal("\n👥 Jogadores únicos: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(totalPlayers)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🎮 Total de jogos: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(totalGames)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🏃 Vitórias Hiders: ").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(String.valueOf(totalHiderWins)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🔍 Vitórias Seekers: ").withStyle(ChatFormatting.RED))
            .append(Component.literal(String.valueOf(totalSeekerWins)).withStyle(ChatFormatting.WHITE));
    }
    
    /**
     * Busca jogador por nome (parcial, case-insensitive)
     */
    public PlayerStats findPlayerByName(String partialName) {
        return playerStats.values().stream()
            .filter(stats -> stats.getLastKnownName().toLowerCase().contains(partialName.toLowerCase()))
            .findFirst()
            .orElse(null);
    }
}