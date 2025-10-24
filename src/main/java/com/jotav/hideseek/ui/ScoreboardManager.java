package com.jotav.hideseek.ui;

import com.jotav.hideseek.game.GameManager;
import com.jotav.hideseek.game.GameState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashSet;
import java.util.Set;

/**
 * Gerencia Scoreboard para mostrar status de times e jogadores
 */
public class ScoreboardManager {
    private static ScoreboardManager instance;
    private MinecraftServer server;
    private Scoreboard scoreboard;
    private Objective hideSeekObjective;
    private PlayerTeam hidersTeam;
    private PlayerTeam seekersTeam;
    private PlayerTeam spectatorsTeam;
    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();
    private boolean scoreboardVisible = false;
    
    private ScoreboardManager() {}
    
    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
        this.scoreboard = server.getScoreboard();
        
        // Reset estado anterior para evitar conflitos
        scoreboardVisible = false;
        trackingPlayers.clear();
        
        initializeScoreboard();
    }
    
    private void initializeScoreboard() {
        if (scoreboard == null) return;
        
        // Remover objetivo anterior se existir
        Objective existingObjective = scoreboard.getObjective("hideseek");
        if (existingObjective != null) {
            scoreboard.removeObjective(existingObjective);
            com.jotav.hideseek.HideSeek.LOGGER.info("Removed existing 'hideseek' objective from scoreboard");
        }
        
        // Criar objetivo principal
        hideSeekObjective = scoreboard.addObjective(
            "hideseek",
            ObjectiveCriteria.DUMMY,
            Component.literal("🎮 Hide & Seek 🎮").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            null
        );
        
        // Só definir como sidebar se deve estar visível
        if (scoreboardVisible) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, hideSeekObjective);
        }
        
        // Criar teams
        createTeams();
    }
    
    private void createTeams() {
        // Limpar teams existentes de forma segura
        PlayerTeam existingHiders = scoreboard.getPlayerTeam("hiders");
        if (existingHiders != null) {
            scoreboard.removePlayerTeam(existingHiders);
        }
        
        PlayerTeam existingSeekers = scoreboard.getPlayerTeam("seekers");
        if (existingSeekers != null) {
            scoreboard.removePlayerTeam(existingSeekers);
        }
        
        PlayerTeam existingSpectators = scoreboard.getPlayerTeam("spectators");
        if (existingSpectators != null) {
            scoreboard.removePlayerTeam(existingSpectators);
        }
        
        // Team Hiders (Verde)
        hidersTeam = scoreboard.addPlayerTeam("hiders");
        hidersTeam.setDisplayName(Component.literal("Hiders").withStyle(ChatFormatting.GREEN));
        hidersTeam.setColor(ChatFormatting.GREEN);
        hidersTeam.setNameTagVisibility(Team.Visibility.HIDE_FOR_OTHER_TEAMS);
        hidersTeam.setCollisionRule(Team.CollisionRule.NEVER);
        
        // Team Seekers (Vermelho)
        seekersTeam = scoreboard.addPlayerTeam("seekers");
        seekersTeam.setDisplayName(Component.literal("Seekers").withStyle(ChatFormatting.RED));
        seekersTeam.setColor(ChatFormatting.RED);
        seekersTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
        seekersTeam.setCollisionRule(Team.CollisionRule.ALWAYS);
        
        // Team Spectators (Cinza)
        spectatorsTeam = scoreboard.addPlayerTeam("spectators");
        spectatorsTeam.setDisplayName(Component.literal("Espectadores").withStyle(ChatFormatting.GRAY));
        spectatorsTeam.setColor(ChatFormatting.GRAY);
        spectatorsTeam.setNameTagVisibility(Team.Visibility.NEVER);
        spectatorsTeam.setCollisionRule(Team.CollisionRule.NEVER);
        
        com.jotav.hideseek.HideSeek.LOGGER.info("Hide & Seek teams created successfully");
    }
    
    /**
     * Atualiza o scoreboard com informações atuais do jogo
     * Versão simplificada focando apenas em teams e título
     */
    public void updateScoreboard() {
        if (hideSeekObjective == null || !scoreboardVisible) return;
        
        GameManager gameManager = GameManager.getInstance();
        GameState currentState = gameManager.getCurrentState();
        
        // Obter contadores
        int hidersCount = gameManager.getPlayerManager().getHidersCount();
        int seekersCount = gameManager.getPlayerManager().getSeekersCount();
        int spectatorsCount = gameManager.getPlayerManager().getSpectatorsCount();
        
        // Atualizar título baseado no estado com contadores
        Component title = switch (currentState) {
            case LOBBY -> Component.literal("⏳ Aguardando Jogadores").withStyle(ChatFormatting.YELLOW);
            case STARTING -> Component.literal("🚀 Iniciando Jogo!").withStyle(ChatFormatting.GOLD);
            case HIDING -> Component.literal("👁 Escondendo (" + hidersCount + " vs " + seekersCount + ")").withStyle(ChatFormatting.GREEN);
            case SEEKING -> Component.literal("🔍 Buscando (" + hidersCount + " vs " + seekersCount + ")").withStyle(ChatFormatting.RED);
            case ENDING -> Component.literal("🏆 Jogo Finalizado").withStyle(ChatFormatting.LIGHT_PURPLE);
        };
        
        hideSeekObjective.setDisplayName(title);
        
        // Scoreboard simplificado - apenas título com contadores integrados
        // As linhas individuais serão adicionadas em versões futuras
        
        // Atualizar teams dos jogadores (cores dos nomes)
        updatePlayerTeams();
        
        // Log para debug
        com.jotav.hideseek.HideSeek.LOGGER.debug("Scoreboard updated - State: {}, Hiders: {}, Seekers: {}, Spectators: {}", 
            currentState, hidersCount, seekersCount, spectatorsCount);
    }
    
    /**
     * Adiciona uma linha ao scoreboard (desabilitado por ora)
     */
    private void addScoreboardLine(String text, int score) {
        // Método desabilitado temporariamente devido a mudanças na API do Minecraft 1.21
        // O scoreboard apenas mostrará o título com contadores integrados
        // TODO: Implementar corretamente com a nova API
    }
    
    /**
     * Limpa todas as pontuações do scoreboard
     */
    private void clearAllScores() {
        if (hideSeekObjective == null) return;
        
        // Limpar de forma mais simples
        try {
            // Resetar o objective completamente
            scoreboard.removeObjective(hideSeekObjective);
            
            // Recriar
            hideSeekObjective = scoreboard.addObjective(
                "hideseek_game", 
                ObjectiveCriteria.DUMMY, 
                Component.literal("Hide & Seek").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                ObjectiveCriteria.RenderType.INTEGER,
                true, // numberFormat
                null // displayAutoUpdate
            );
            
            // Definir como displaySlot novamente se estava ativo
            if (scoreboardVisible) {
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, hideSeekObjective);
            }
            
        } catch (Exception e) {
            com.jotav.hideseek.HideSeek.LOGGER.debug("Error clearing scoreboard: {}", e.getMessage());
        }
    }
    
    /**
     * Atualiza os teams dos jogadores
     */
    private void updatePlayerTeams() {
        if (scoreboard == null) return;
        
        GameManager gameManager = GameManager.getInstance();
        
        // Adicionar Hiders ao team verde
        for (ServerPlayer hider : gameManager.getPlayerManager().getHiders()) {
            scoreboard.addPlayerToTeam(hider.getScoreboardName(), hidersTeam);
        }
        
        // Adicionar Seekers ao team vermelho
        for (ServerPlayer seeker : gameManager.getPlayerManager().getSeekers()) {
            scoreboard.addPlayerToTeam(seeker.getScoreboardName(), seekersTeam);
        }
        
        // Adicionar Spectators ao team cinza
        for (ServerPlayer spectator : gameManager.getPlayerManager().getSpectators()) {
            scoreboard.addPlayerToTeam(spectator.getScoreboardName(), spectatorsTeam);
        }
    }
    
    /**
     * Adiciona jogador ao tracking (recebe atualizações do scoreboard)
     */
    public void addPlayer(ServerPlayer player) {
        trackingPlayers.add(player);
        updateScoreboard(); // Atualizar imediatamente
    }
    
    /**
     * Remove jogador do tracking
     */
    public void removePlayer(ServerPlayer player) {
        trackingPlayers.remove(player);
        
        // Remover de todos os teams com tratamento de erro
        try {
            if (hidersTeam != null) scoreboard.removePlayerFromTeam(player.getScoreboardName(), hidersTeam);
        } catch (Exception e) {
            // Jogador pode não estar no team
        }
        try {
            if (seekersTeam != null) scoreboard.removePlayerFromTeam(player.getScoreboardName(), seekersTeam);
        } catch (Exception e) {
            // Jogador pode não estar no team
        }
        try {
            if (spectatorsTeam != null) scoreboard.removePlayerFromTeam(player.getScoreboardName(), spectatorsTeam);
        } catch (Exception e) {
            // Jogador pode não estar no team
        }
    }
    
    /**
     * Limpa completamente o scoreboard
     */
    public void clearScoreboard() {
        if (hideSeekObjective != null) {
            scoreboard.removeObjective(hideSeekObjective);
            hideSeekObjective = null;
        }
        
        if (hidersTeam != null) {
            scoreboard.removePlayerTeam(hidersTeam);
            hidersTeam = null;
        }
        
        if (seekersTeam != null) {
            scoreboard.removePlayerTeam(seekersTeam);
            seekersTeam = null;
        }
        
        if (spectatorsTeam != null) {
            scoreboard.removePlayerTeam(spectatorsTeam);
            spectatorsTeam = null;
        }
        
        trackingPlayers.clear();
        scoreboardVisible = false;
    }
    
    /**
     * Mostra o scoreboard para todos os jogadores
     */
    public void showScoreboard() {
        scoreboardVisible = true;
        if (hideSeekObjective != null && scoreboard != null) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, hideSeekObjective);
            updateScoreboard();
        }
    }
    
    /**
     * Oculta o scoreboard de todos os jogadores
     */
    public void hideScoreboard() {
        scoreboardVisible = false;
        if (scoreboard != null) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
        }
    }
    
    /**
     * Oculta scoreboard e limpa teams (para final de jogo)
     */
    public void hideScoreboardAndClearTeams() {
        hideScoreboard();
        clearScoreboard();
    }
    
    /**
     * Verifica se o scoreboard está visível
     */
    public boolean isScoreboardVisible() {
        return scoreboardVisible;
    }
}