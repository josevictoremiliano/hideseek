package com.jotav.hideseek.game;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.HideSeek;
import com.jotav.hideseek.chat.ChatManager;
import com.jotav.hideseek.config.GameConfig;
import com.jotav.hideseek.effects.EffectsManager;
import com.jotav.hideseek.stats.StatsManager;
import com.jotav.hideseek.ui.BossBarManager;
import com.jotav.hideseek.ui.ScoreboardManager;
import com.jotav.hideseek.util.ConfigHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Gerenciador central do estado do jogo Hide and Seek
 */
public class GameManager {
    private static GameManager instance;
    
    private GameState currentState = GameState.LOBBY;
    private final PlayerManager playerManager = new PlayerManager();
    private final ChatManager chatManager = ChatManager.getInstance();
    private final StatsManager statsManager = StatsManager.getInstance();
    private final GameConfig gameConfig = GameConfig.getInstance();
    private MinecraftServer server;
    
    // Configurações do jogo vêm do Config.java
    
    // Timer do jogo
    private Timer gameTimer;
    
    private GameManager() {}
    
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
        
        // Reset estado para evitar problemas de reinicialização
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        
        currentState = GameState.LOBBY;
        playerManager.removeAllPlayers(); // Limpar jogadores fantasma
        
        // Inicializar managers de UI
        BossBarManager.getInstance().setServer(server);
        ScoreboardManager.getInstance().setServer(server);
        
        HideSeek.LOGGER.info("GameManager initialized with server");
    }
    
    public MinecraftServer getServer() {
        return server;
    }
    
    /**
     * Jogador tenta entrar no jogo
     */
    public boolean joinGame(ServerPlayer player) {
        if (currentState != GameState.LOBBY) {
            chatManager.gameAlreadyInProgress(player);
            return false;
        }
        
        boolean joined = playerManager.joinLobby(player);
        if (joined) {
            // Atualizar estatísticas com nome atual do jogador
            statsManager.updatePlayerName(player);
            
            // Teleportar para lobby spawn
            EffectsManager.getInstance().teleportToLobby(player);
            
            // Adicionar aos sistemas de UI
            BossBarManager.getInstance().addPlayer(player);
            ScoreboardManager.getInstance().addPlayer(player);
            
            // Atualizar UI para todos
            ScoreboardManager.getInstance().updateScoreboard();
            
            // Enviar mensagem de chat
            int totalPlayers = playerManager.getTotalPlayerCount();
            int minRequired = Config.MIN_PLAYERS.get();
            chatManager.playerJoinedGame(server, player, totalPlayers, minRequired);
            
            HideSeek.LOGGER.info("Player {} joined the game lobby", player.getName().getString());
        }
        return joined;
    }
    
    /**
     * Jogador sai do jogo
     */
    public boolean leaveGame(ServerPlayer player) {
        boolean left = playerManager.leaveGame(player);
        if (left) {
            // Teleportar para lobby spawn
            EffectsManager.getInstance().teleportToLobby(player);
            
            // Remover dos sistemas de UI
            BossBarManager.getInstance().removePlayer(player);
            ScoreboardManager.getInstance().removePlayer(player);
            
            // Atualizar UI para todos
            ScoreboardManager.getInstance().updateScoreboard();
            
            // Enviar mensagem de chat
            int remainingPlayers = playerManager.getTotalPlayerCount();
            chatManager.playerLeftGame(server, player, remainingPlayers);
            
            HideSeek.LOGGER.info("Player {} left the game", player.getName().getString());
        }
        return left;
    }
    
    /**
     * Força início do jogo
     */
    public boolean startGame() {
        if (currentState != GameState.LOBBY) {
            return false;
        }
        
        if (playerManager.getLobbyCount() < Config.MIN_PLAYERS.get()) {
            return false; // Mensagem será enviada pelo comando
        }
        
        transitionToStarting();
        return true;
    }
    
    /**
     * Para o jogo completamente
     */
    public void stopGame() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        
        // Parar sistemas de UI
        BossBarManager.getInstance().stopTimer();
        
        // Enviar mensagem de reset
        chatManager.gameReset(server);
        
        // Ocultar scoreboard e limpar teams
        ScoreboardManager.getInstance().hideScoreboardAndClearTeams();
        
        // Limpar efeitos de todos os jogadores e restaurar gamemodes
        EffectsManager.getInstance().clearAllEffectsAndRestoreGameModes();
        
        // Teleportar todos para lobby e remover dos teams
        for (ServerPlayer player : playerManager.getHiders()) {
            EffectsManager.getInstance().teleportToLobby(player);
            BossBarManager.getInstance().removePlayer(player);
            ScoreboardManager.getInstance().removePlayer(player);
        }
        for (ServerPlayer player : playerManager.getSeekers()) {
            EffectsManager.getInstance().teleportToLobby(player);
            BossBarManager.getInstance().removePlayer(player);
            ScoreboardManager.getInstance().removePlayer(player);
        }
        for (ServerPlayer player : playerManager.getSpectators()) {
            EffectsManager.getInstance().teleportToLobby(player);
            BossBarManager.getInstance().removePlayer(player);
            ScoreboardManager.getInstance().removePlayer(player);
        }
        
        playerManager.resetAll();
        currentState = GameState.LOBBY;
        
        // Atualizar UI
        ScoreboardManager.getInstance().updateScoreboard();
        
        // TODO: Limpar inventários
        
        HideSeek.LOGGER.info("Game stopped and reset to lobby");
    }
    
    /**
     * Registra estatísticas do final do jogo
     */
    private void recordGameStats(boolean seekersWin) {
        // Calcular tempo de jogo para cada jogador
        long hideTimeSeconds = Config.HIDE_TIME.get();
        long totalGameTimeSeconds = hideTimeSeconds + (System.currentTimeMillis() - phaseStartTime) / 1000;
        
        // Registrar vitórias/derrotas
        if (seekersWin) {
            // Seekers venceram
            for (ServerPlayer seeker : playerManager.getSeekers()) {
                statsManager.recordWin(seeker, false);
                statsManager.recordSeekingTime(seeker, totalGameTimeSeconds - hideTimeSeconds);
            }
            
            // Hiders perderam (incluindo espectadores que foram capturados)
            for (ServerPlayer hider : playerManager.getHiders()) {
                statsManager.recordLoss(hider, true);
                statsManager.recordHidingTime(hider, totalGameTimeSeconds);
            }
            for (ServerPlayer spectator : playerManager.getSpectators()) {
                // Espectadores são ex-Hiders que foram capturados
                statsManager.recordLoss(spectator, true);
                // Tempo de sobrevivência foi menor que o total
            }
        } else {
            // Hiders venceram
            for (ServerPlayer hider : playerManager.getHiders()) {
                statsManager.recordWin(hider, true);
                statsManager.recordHidingTime(hider, totalGameTimeSeconds);
            }
            
            // Seekers perderam
            for (ServerPlayer seeker : playerManager.getSeekers()) {
                statsManager.recordLoss(seeker, false);
                statsManager.recordSeekingTime(seeker, totalGameTimeSeconds - hideTimeSeconds);
            }
            
            // Espectadores (ex-Hiders capturados) também perderam
            for (ServerPlayer spectator : playerManager.getSpectators()) {
                statsManager.recordLoss(spectator, true);
            }
        }
        
        // Salvar estatísticas
        statsManager.saveStats();
    }
    
    // Variável para rastrear início da fase atual
    private long phaseStartTime;
    
    /**
     * Transição: LOBBY → STARTING
     */
    private void transitionToStarting() {
        currentState = GameState.STARTING;
        HideSeek.LOGGER.info("Game starting countdown...");
        
        // Mostrar scoreboard quando o jogo começar
        ScoreboardManager.getInstance().showScoreboard();
        
        // Iniciar contagem regressiva no BossBar
        BossBarManager.getInstance().startPhaseTimer(GameState.STARTING, Config.STARTING_COUNTDOWN.get());
        
        // Atualizar scoreboard
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Enviar mensagem de início
        chatManager.gameStartingCountdown(server, Config.STARTING_COUNTDOWN.get());
        
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                transitionToHiding();
            }
        }, Config.STARTING_COUNTDOWN.get() * 1000L);
    }
    
    /**
     * Transição: STARTING → HIDING
     */
    private void transitionToHiding() {
        currentState = GameState.HIDING;
        phaseStartTime = System.currentTimeMillis();
        
        // Distribuir jogadores em times
        playerManager.assignTeams(Config.MIN_HIDERS.get(), Config.MAX_HIDERS.get());
        
        // Registrar início do jogo para estatísticas
        for (ServerPlayer hider : playerManager.getHiders()) {
            statsManager.recordGameStart(hider, true);
        }
        for (ServerPlayer seeker : playerManager.getSeekers()) {
            statsManager.recordGameStart(seeker, false);
        }
        
        // Enviar mensagem de times formados
        chatManager.teamsAssigned(server, playerManager.getHiders().size(), playerManager.getSeekers().size());
        
        // Enviar mensagem de início da fase de esconder
        chatManager.hidingPhaseStarted(server, Config.HIDE_TIME.get());
        
        // Iniciar timer da fase HIDING
        BossBarManager.getInstance().startPhaseTimer(GameState.HIDING, Config.HIDE_TIME.get());
        
        // Atualizar scoreboard com novos times
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Verificar se configurações essenciais estão definidas
        if (!isGameConfigured()) {
            chatManager.configurationMissing(server, getMissingConfigurations());
            currentState = GameState.LOBBY;
            return;
        }
        
        // Teleportar Seekers para seeker spawn e aplicar efeitos
        boolean seekerTeleportSuccess = EffectsManager.getInstance().teleportSeekersToSpawn(playerManager.getSeekers());
        if (!seekerTeleportSuccess) {
            chatManager.configurationMissing(server, "Seeker spawn não configurado! Use /hns set seekerspawn");
            currentState = GameState.LOBBY;
            return;
        }
        EffectsManager.getInstance().applySeekerEffects(playerManager.getSeekers());
        
        // Aplicar efeitos para Hiders (Adventure Mode + Jump Boost temporário)
        EffectsManager.getInstance().applyHiderEffects(playerManager.getHiders());
        
        // Teleportar Hiders para posições aleatórias (espalhar pelo mapa)
        BlockPos lobbyPos = getLobbySpawn();
        if (lobbyPos != null) {
            for (ServerPlayer hider : playerManager.getHiders()) {
                // Dar spawn aleatório em um raio de 50 blocos do lobby
                BlockPos randomPos = lobbyPos.offset(
                    (int)(Math.random() * 100 - 50), // -50 a +50 em X
                    0,
                    (int)(Math.random() * 100 - 50)  // -50 a +50 em Z
                );
                EffectsManager.getInstance().safeTeleport(hider, randomPos, hider.level().dimension());
            }
        } else {
            chatManager.configurationMissing(server, "Lobby spawn não configurado! Use /hns set lobby");
            currentState = GameState.LOBBY;
            return;
        }
        
        HideSeek.LOGGER.info("Hiding phase started - {} Hiders, {} Seekers", 
                            playerManager.getHidersCount(), playerManager.getSeekersCount());
        
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                transitionToSeeking();
            }
        }, Config.HIDE_TIME.get() * 1000L);
    }
    
    /**
     * Transição: HIDING → SEEKING
     */
    private void transitionToSeeking() {
        currentState = GameState.SEEKING;
        phaseStartTime = System.currentTimeMillis();
        
        // Iniciar timer da fase SEEKING
        BossBarManager.getInstance().startPhaseTimer(GameState.SEEKING, Config.SEEK_TIME.get());
        
        // Atualizar scoreboard
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Remover efeitos dos Seekers (liberá-los)
        EffectsManager.getInstance().removeSeekerEffects(playerManager.getSeekers());
        
        // Remover Jump Boost dos Hiders (eles perdem a habilidade de pulo extra)
        EffectsManager.getInstance().removeHiderJumpBoost(playerManager.getHiders());
        
        // Anunciar liberação dos Seekers
        chatManager.seekingPhaseStarted(server, Config.SEEK_TIME.get(), playerManager.getHidersCount());
        
        HideSeek.LOGGER.info("Seeking phase started");
        
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame(false); // Timeout - Hiders vencem
            }
        }, Config.SEEK_TIME.get() * 1000L);
    }
    
    /**
     * Termina o jogo
     * @param seekersWin true se Seekers venceram, false se Hiders venceram
     */
    public void endGame(boolean seekersWin) {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        
        currentState = GameState.ENDING;
        
        // Mostrar resultado no BossBar
        BossBarManager.getInstance().showGameResult(seekersWin);
        
        // Atualizar scoreboard
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Registrar estatísticas do jogo
        recordGameStats(seekersWin);
        
        // Enviar mensagens de vitória
        if (seekersWin) {
            chatManager.seekersWin(server, playerManager.getSeekers());
        } else {
            chatManager.hidersWin(server, playerManager.getHiders(), playerManager.getHidersCount());
        }
        
        HideSeek.LOGGER.info("Game ended - {} won", seekersWin ? "Seekers" : "Hiders");
        
        // Anunciar retorno ao lobby
        chatManager.returningToLobby(server, 10);
        
        // Auto-retorno ao lobby após 10 segundos
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                returnToLobby();
            }
        }, 10000);
    }
    
    /**
     * Retorna ao lobby
     */
    private void returnToLobby() {
        // Ocultar scoreboard e limpar teams quando voltar ao lobby
        ScoreboardManager.getInstance().hideScoreboardAndClearTeams();
        stopGame(); // Reset completo
    }
    
    /**
     * Hider foi capturado
     */
    public void captureHider(ServerPlayer hider, ServerPlayer seeker) {
        if (currentState != GameState.SEEKING) {
            return;
        }
        
        if (playerManager.captureHider(hider)) {
            // Registrar estatísticas de captura
            statsManager.recordPlayerCaptured(hider);
            statsManager.recordPlayerMadeCapture(seeker);
            
            // Teleportar hider capturado para posição de espectador (lobby por enquanto)
            EffectsManager.getInstance().teleportToLobby(hider);
            
            // Atualizar UI imediatamente
            ScoreboardManager.getInstance().updateScoreboard();
            
            // Anunciar captura no chat
            int hidersRemaining = playerManager.getHidersCount();
            chatManager.playerCaptured(server, hider, seeker, hidersRemaining);
            
            // TODO: Aplicar modo espectador real
            
            HideSeek.LOGGER.info("Player {} captured by {}", hider.getName().getString(), seeker.getName().getString());
            
            // Verificar condição de vitória
            if (playerManager.getHidersCount() == 0) {
                endGame(true); // Todos capturados - Seekers vencem
            }
        }
    }
    
    /**
     * Remove todos os jogadores do jogo (comando leaveall)
     */
    public int removeAllPlayers() {
        // Parar sistemas de UI
        BossBarManager.getInstance().stopTimer();
        ScoreboardManager.getInstance().hideScoreboardAndClearTeams();
        
        // Limpar efeitos de todos os jogadores
        EffectsManager.getInstance().clearAllEffects();
        
        // Obter todos os jogadores antes de limpar
        Set<ServerPlayer> allPlayers = playerManager.removeAllPlayers();
        
        // Teleportar todos para lobby e remover do UI
        for (ServerPlayer player : allPlayers) {
            try {
                EffectsManager.getInstance().teleportToLobby(player);
                BossBarManager.getInstance().removePlayer(player);
                ScoreboardManager.getInstance().removePlayer(player);
            } catch (Exception e) {
                HideSeek.LOGGER.warn("Error removing player {} from UI systems: {}", 
                                   player.getName().getString(), e.getMessage());
            }
        }
        
        // Resetar estado
        currentState = GameState.LOBBY;
        
        // Parar timer se ativo
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        
        HideSeek.LOGGER.info("All {} players removed from game by admin command", allPlayers.size());
        return allPlayers.size();
    }
    
    // Getters
    public GameState getCurrentState() { return currentState; }
    public PlayerManager getPlayerManager() { return playerManager; }
    
    // Métodos para gerenciar spawn points via configuração persistente
    public void setLobbySpawn(BlockPos pos, ResourceKey<Level> dimension) { 
        gameConfig.setLobbySpawn(pos, dimension);
        HideSeek.LOGGER.info("Lobby spawn set and saved");
    }
    
    public void setSeekerSpawn(BlockPos pos, ResourceKey<Level> dimension) { 
        gameConfig.setSeekerSpawn(pos, dimension);
        HideSeek.LOGGER.info("Seeker spawn set and saved");
    }
    
    public void setMapBoundary(BlockPos min, BlockPos max) { 
        gameConfig.setMapBoundary(min, max);
        HideSeek.LOGGER.info("Map boundary set and saved");
    }
    
    public BlockPos getLobbySpawn() { 
        return gameConfig.getLobbySpawn();
    }
    
    public BlockPos getSeekerSpawn() { 
        return gameConfig.getSeekerSpawn();
    }
    
    public BlockPos getMapBoundaryMin() { 
        return gameConfig.getMapBoundaryMin();
    }
    
    public BlockPos getMapBoundaryMax() { 
        return gameConfig.getMapBoundaryMax();
    }
    
    /**
     * Verifica se o jogo está totalmente configurado
     */
    public boolean isGameConfigured() {
        return gameConfig.isFullyConfigured();
    }
    
    /**
     * Retorna string com configurações faltantes
     */
    public String getMissingConfigurations() {
        return gameConfig.getMissingConfigurations();
    }
}