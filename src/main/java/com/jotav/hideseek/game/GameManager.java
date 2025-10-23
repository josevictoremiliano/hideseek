package com.jotav.hideseek.game;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.HideSeek;
import com.jotav.hideseek.effects.EffectsManager;
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
    private MinecraftServer server;
    
    // Configurações do jogo vêm do Config.java
    
    // Pontos de spawn
    private BlockPos lobbySpawn;
    private BlockPos seekerSpawn;
    private BlockPos mapBoundaryMin;
    private BlockPos mapBoundaryMax;
    
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
    
    /**
     * Jogador tenta entrar no jogo
     */
    public boolean joinGame(ServerPlayer player) {
        if (currentState != GameState.LOBBY) {
            // TODO: Enviar mensagem de erro
            return false;
        }
        
        boolean joined = playerManager.joinLobby(player);
        if (joined) {
            // Teleportar para lobby spawn
            EffectsManager.getInstance().teleportToLobby(player);
            
            // Adicionar aos sistemas de UI
            BossBarManager.getInstance().addPlayer(player);
            ScoreboardManager.getInstance().addPlayer(player);
            
            // Atualizar UI para todos
            ScoreboardManager.getInstance().updateScoreboard();
            
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
            // TODO: Mensagem de erro - mínimo jogadores insuficientes
            return false;
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
        
        // Ocultar scoreboard
        ScoreboardManager.getInstance().hideScoreboard();
        
        // Limpar efeitos de todos os jogadores
        EffectsManager.getInstance().clearAllEffects();
        
        // Teleportar todos para lobby
        for (ServerPlayer player : playerManager.getHiders()) {
            EffectsManager.getInstance().teleportToLobby(player);
        }
        for (ServerPlayer player : playerManager.getSeekers()) {
            EffectsManager.getInstance().teleportToLobby(player);
        }
        for (ServerPlayer player : playerManager.getSpectators()) {
            EffectsManager.getInstance().teleportToLobby(player);
        }
        
        playerManager.resetAll();
        currentState = GameState.LOBBY;
        
        // Atualizar UI
        ScoreboardManager.getInstance().updateScoreboard();
        
        // TODO: Limpar inventários
        
        HideSeek.LOGGER.info("Game stopped and reset to lobby");
    }
    
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
        
        // Distribuir jogadores em times
        playerManager.assignTeams(Config.MIN_HIDERS.get(), Config.MAX_HIDERS.get());
        
        // Iniciar timer da fase HIDING
        BossBarManager.getInstance().startPhaseTimer(GameState.HIDING, Config.HIDE_TIME.get());
        
        // Atualizar scoreboard com novos times
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Teleportar Seekers para seeker spawn e aplicar efeitos
        EffectsManager.getInstance().teleportSeekersToSpawn(playerManager.getSeekers());
        EffectsManager.getInstance().applySeekerEffects(playerManager.getSeekers());
        
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
        
        // Iniciar timer da fase SEEKING
        BossBarManager.getInstance().startPhaseTimer(GameState.SEEKING, Config.SEEK_TIME.get());
        
        // Atualizar scoreboard
        ScoreboardManager.getInstance().updateScoreboard();
        
        // Remover efeitos dos Seekers (liberá-los)
        EffectsManager.getInstance().removeSeekerEffects(playerManager.getSeekers());
        
        // TODO: Anunciar liberação dos Seekers via chat/título
        
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
        
        HideSeek.LOGGER.info("Game ended - {} won", seekersWin ? "Seekers" : "Hiders");
        
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
        // Ocultar scoreboard quando voltar ao lobby
        ScoreboardManager.getInstance().hideScoreboard();
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
            // Teleportar hider capturado para posição de espectador (lobby por enquanto)
            EffectsManager.getInstance().teleportToLobby(hider);
            
            // Atualizar UI imediatamente
            ScoreboardManager.getInstance().updateScoreboard();
            
            // TODO: Aplicar modo espectador real
            // TODO: Anunciar captura no chat
            
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
        ScoreboardManager.getInstance().hideScoreboard();
        
        // Limpar efeitos de todos os jogadores
        EffectsManager.getInstance().clearAllEffects();
        
        // Obter todos os jogadores antes de limpar
        Set<ServerPlayer> allPlayers = playerManager.removeAllPlayers();
        
        // Teleportar todos para lobby e remover do UI
        for (ServerPlayer player : allPlayers) {
            EffectsManager.getInstance().teleportToLobby(player);
            BossBarManager.getInstance().removePlayer(player);
            ScoreboardManager.getInstance().removePlayer(player);
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
    
    // Métodos para gerenciar spawn points via configuração
    public void setLobbySpawn(BlockPos pos, ResourceKey<Level> dimension) { 
        String configValue = ConfigHelper.positionToString(pos, dimension);
        // TODO: Salvar no arquivo de configuração
        this.lobbySpawn = pos;
        HideSeek.LOGGER.info("Lobby spawn set to: {}", configValue);
    }
    
    public void setSeekerSpawn(BlockPos pos, ResourceKey<Level> dimension) { 
        String configValue = ConfigHelper.positionToString(pos, dimension);
        // TODO: Salvar no arquivo de configuração  
        this.seekerSpawn = pos;
        HideSeek.LOGGER.info("Seeker spawn set to: {}", configValue);
    }
    
    public void setMapBoundary(BlockPos min, BlockPos max) { 
        this.mapBoundaryMin = min; 
        this.mapBoundaryMax = max;
        // TODO: Salvar no arquivo de configuração
        HideSeek.LOGGER.info("Map boundary set: {} to {}", 
            ConfigHelper.simplePositionToString(min), 
            ConfigHelper.simplePositionToString(max));
    }
    
    public BlockPos getLobbySpawn() { 
        if (lobbySpawn == null) {
            lobbySpawn = ConfigHelper.stringToPosition(Config.LOBBY_SPAWN.get());
        }
        return lobbySpawn; 
    }
    
    public BlockPos getSeekerSpawn() { 
        if (seekerSpawn == null) {
            seekerSpawn = ConfigHelper.stringToPosition(Config.SEEKER_SPAWN.get());
        }
        return seekerSpawn; 
    }
    
    public BlockPos getMapBoundaryMin() { 
        if (mapBoundaryMin == null) {
            mapBoundaryMin = ConfigHelper.stringToSimplePosition(Config.MAP_BOUNDARY_MIN.get());
        }
        return mapBoundaryMin; 
    }
    
    public BlockPos getMapBoundaryMax() { 
        if (mapBoundaryMax == null) {
            mapBoundaryMax = ConfigHelper.stringToSimplePosition(Config.MAP_BOUNDARY_MAX.get());
        }
        return mapBoundaryMax; 
    }
}