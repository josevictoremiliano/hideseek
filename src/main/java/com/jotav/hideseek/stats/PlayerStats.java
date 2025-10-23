package com.jotav.hideseek.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;

/**
 * Estatísticas individuais de um jogador no Hide and Seek
 */
public class PlayerStats {
    // Identificação
    private UUID playerUuid;
    private String lastKnownName;
    private long firstPlayTime;
    private long lastPlayTime;
    
    // Estatísticas de jogos
    private int gamesPlayed = 0;
    private int gamesWon = 0;
    private int gamesLost = 0;
    
    // Estatísticas como Hider
    private int gamesAsHider = 0;
    private int hiderWins = 0;
    private int hiderLosses = 0;
    private int timesCaptured = 0;
    private long totalHidingTimeSeconds = 0; // Tempo total escondido
    private long longestSurvivalTimeSeconds = 0; // Maior tempo sobrevivido em uma partida
    
    // Estatísticas como Seeker  
    private int gamesAsSeeker = 0;
    private int seekerWins = 0;
    private int seekerLosses = 0;
    private int playersCaptured = 0;
    private long totalSeekingTimeSeconds = 0; // Tempo total procurando
    private long fastestCaptureTimeSeconds = Long.MAX_VALUE; // Menor tempo para primeira captura
    
    // Streaks e recordes
    private int currentWinStreak = 0;
    private int longestWinStreak = 0;
    private int currentLossStreak = 0;
    private int longestLossStreak = 0;
    
    // Construtor padrão para GSON
    public PlayerStats() {}
    
    public PlayerStats(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.lastKnownName = playerName;
        this.firstPlayTime = System.currentTimeMillis();
        this.lastPlayTime = this.firstPlayTime;
    }
    
    // ================== GETTERS ==================
    
    public UUID getPlayerUuid() { return playerUuid; }
    public String getLastKnownName() { return lastKnownName; }
    public long getFirstPlayTime() { return firstPlayTime; }
    public long getLastPlayTime() { return lastPlayTime; }
    
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public int getGamesLost() { return gamesLost; }
    public double getWinRate() { 
        return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0.0; 
    }
    
    public int getGamesAsHider() { return gamesAsHider; }
    public int getHiderWins() { return hiderWins; }
    public int getHiderLosses() { return hiderLosses; }
    public double getHiderWinRate() {
        return gamesAsHider > 0 ? (double) hiderWins / gamesAsHider * 100 : 0.0;
    }
    public int getTimesCaptured() { return timesCaptured; }
    public long getTotalHidingTimeSeconds() { return totalHidingTimeSeconds; }
    public long getLongestSurvivalTimeSeconds() { return longestSurvivalTimeSeconds; }
    
    public int getGamesAsSeeker() { return gamesAsSeeker; }
    public int getSeekerWins() { return seekerWins; }
    public int getSeekerLosses() { return seekerLosses; }
    public double getSeekerWinRate() {
        return gamesAsSeeker > 0 ? (double) seekerWins / gamesAsSeeker * 100 : 0.0;
    }
    public int getPlayersCaptured() { return playersCaptured; }
    public long getTotalSeekingTimeSeconds() { return totalSeekingTimeSeconds; }
    public long getFastestCaptureTimeSeconds() { 
        return fastestCaptureTimeSeconds == Long.MAX_VALUE ? 0 : fastestCaptureTimeSeconds; 
    }
    
    public int getCurrentWinStreak() { return currentWinStreak; }
    public int getLongestWinStreak() { return longestWinStreak; }
    public int getCurrentLossStreak() { return currentLossStreak; }
    public int getLongestLossStreak() { return longestLossStreak; }
    
    // ================== SETTERS PARA INFORMAÇÕES BÁSICAS ==================
    
    public void updateLastKnownName(String name) {
        this.lastKnownName = name;
        this.lastPlayTime = System.currentTimeMillis();
    }
    
    // ================== MÉTODOS PARA ATUALIZAR ESTATÍSTICAS ==================
    
    /**
     * Registra o início de um jogo
     */
    public void startGame(boolean isHider) {
        gamesPlayed++;
        if (isHider) {
            gamesAsHider++;
        } else {
            gamesAsSeeker++;
        }
        updateLastPlayTime();
    }
    
    /**
     * Registra uma vitória
     */
    public void recordWin(boolean wasHider) {
        gamesWon++;
        if (wasHider) {
            hiderWins++;
        } else {
            seekerWins++;
        }
        
        // Atualizar streaks
        currentWinStreak++;
        currentLossStreak = 0;
        if (currentWinStreak > longestWinStreak) {
            longestWinStreak = currentWinStreak;
        }
        updateLastPlayTime();
    }
    
    /**
     * Registra uma derrota
     */
    public void recordLoss(boolean wasHider) {
        gamesLost++;
        if (wasHider) {
            hiderLosses++;
        } else {
            seekerLosses++;
        }
        
        // Atualizar streaks
        currentLossStreak++;
        currentWinStreak = 0;
        if (currentLossStreak > longestLossStreak) {
            longestLossStreak = currentLossStreak;
        }
        updateLastPlayTime();
    }
    
    /**
     * Registra que o jogador foi capturado
     */
    public void recordCapture() {
        timesCaptured++;
        updateLastPlayTime();
    }
    
    /**
     * Registra uma captura feita pelo jogador
     */
    public void recordPlayerCaptured() {
        playersCaptured++;
        updateLastPlayTime();
    }
    
    /**
     * Atualiza tempo total escondido
     */
    public void addHidingTime(long seconds) {
        totalHidingTimeSeconds += seconds;
        if (seconds > longestSurvivalTimeSeconds) {
            longestSurvivalTimeSeconds = seconds;
        }
        updateLastPlayTime();
    }
    
    /**
     * Atualiza tempo total procurando
     */
    public void addSeekingTime(long seconds) {
        totalSeekingTimeSeconds += seconds;
        updateLastPlayTime();
    }
    
    /**
     * Registra tempo para primeira captura (se for menor que o recorde)
     */
    public void recordCaptureTime(long seconds) {
        if (seconds < fastestCaptureTimeSeconds) {
            fastestCaptureTimeSeconds = seconds;
        }
        updateLastPlayTime();
    }
    
    private void updateLastPlayTime() {
        this.lastPlayTime = System.currentTimeMillis();
    }
    
    // ================== MÉTODOS DE FORMATAÇÃO ==================
    
    /**
     * Converte segundos em formato legível (ex: "2m 30s")
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Gera componente de texto com estatísticas resumidas do jogador
     */
    public Component getSummaryComponent() {
        return Component.literal("")
            .append(Component.literal("=== Estatísticas de " + lastKnownName + " ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("\n📊 Geral: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.format("%d jogos, %.1f%% vitórias", gamesPlayed, getWinRate())).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🏃 Como Hider: ").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(String.format("%d jogos, %d vitórias, %d capturas", gamesAsHider, hiderWins, timesCaptured)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🔍 Como Seeker: ").withStyle(ChatFormatting.RED))
            .append(Component.literal(String.format("%d jogos, %d vitórias, %d capturas feitas", gamesAsSeeker, seekerWins, playersCaptured)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n🔥 Streak: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(String.format("%d vitórias seguidas (recorde: %d)", currentWinStreak, longestWinStreak)).withStyle(ChatFormatting.WHITE));
    }
    
    /**
     * Gera componente de texto com estatísticas detalhadas
     */
    public Component getDetailedComponent() {
        return Component.literal("")
            .append(getSummaryComponent())
            .append(Component.literal("\n\n📈 Detalhes Hider:").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal("\n  • Taxa de vitória: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format("%.1f%%", getHiderWinRate())).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  • Tempo total escondido: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatTime(totalHidingTimeSeconds)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  • Maior sobrevivência: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatTime(longestSurvivalTimeSeconds)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n\n🎯 Detalhes Seeker:").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal("\n  • Taxa de vitória: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format("%.1f%%", getSeekerWinRate())).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  • Tempo total procurando: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatTime(totalSeekingTimeSeconds)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  • Captura mais rápida: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatTime(getFastestCaptureTimeSeconds())).withStyle(ChatFormatting.WHITE));
    }
    
    // ================== SERIALIZAÇÃO ==================
    
    /**
     * Converte para JSON
     */
    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    
    /**
     * Cria a partir de JSON
     */
    public static PlayerStats fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, PlayerStats.class);
    }
}