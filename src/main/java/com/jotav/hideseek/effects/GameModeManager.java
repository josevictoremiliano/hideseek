package com.jotav.hideseek.effects;

import com.jotav.hideseek.HideSeek;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador de gamemodes para o Hide and Seek
 */
public class GameModeManager {
    private static GameModeManager instance;
    
    // Armazena o gamemode original de cada jogador
    private final Map<UUID, GameType> originalGameModes = new HashMap<>();
    
    private GameModeManager() {}
    
    public static GameModeManager getInstance() {
        if (instance == null) {
            instance = new GameModeManager();
        }
        return instance;
    }
    
    /**
     * Salva o gamemode original do jogador e muda para Adventure
     */
    public void setGameModeToAdventure(ServerPlayer player) {
        // Salvar gamemode original se ainda não salvou
        if (!originalGameModes.containsKey(player.getUUID())) {
            originalGameModes.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
            HideSeek.LOGGER.debug("Saved original gamemode for {}: {}", 
                                player.getName().getString(), 
                                player.gameMode.getGameModeForPlayer());
        }
        
        // Mudar para Adventure Mode
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            player.setGameMode(GameType.ADVENTURE);
            HideSeek.LOGGER.debug("Set {} to Adventure mode", player.getName().getString());
        }
    }
    
    /**
     * Restaura o gamemode original do jogador
     */
    public void restoreOriginalGameMode(ServerPlayer player) {
        GameType originalMode = originalGameModes.remove(player.getUUID());
        
        if (originalMode != null) {
            player.setGameMode(originalMode);
            HideSeek.LOGGER.debug("Restored gamemode for {}: {}", 
                                player.getName().getString(), originalMode);
        } else {
            // Se não há gamemode salvo, usar Survival como padrão
            player.setGameMode(GameType.SURVIVAL);
            HideSeek.LOGGER.debug("No saved gamemode for {}, set to Survival", 
                                player.getName().getString());
        }
    }
    
    /**
     * Restaura gamemode original para todos os jogadores tracked
     */
    public void restoreAllGameModes() {
        // Criar uma cópia do mapa para evitar ConcurrentModificationException
        Map<UUID, GameType> copy = new HashMap<>(originalGameModes);
        
        for (Map.Entry<UUID, GameType> entry : copy.entrySet()) {
            UUID playerUuid = entry.getKey();
            GameType originalMode = entry.getValue();
            
            // Tentar encontrar o jogador online
            ServerPlayer player = findPlayerByUuid(playerUuid);
            if (player != null) {
                restoreOriginalGameMode(player);
            } else {
                // Jogador offline, remover do mapa
                originalGameModes.remove(playerUuid);
                HideSeek.LOGGER.debug("Removed offline player {} from gamemode tracking", playerUuid);
            }
        }
    }
    
    /**
     * Limpa todos os gamemodes salvos (para reset completo)
     */
    public void clearAllSavedGameModes() {
        originalGameModes.clear();
        HideSeek.LOGGER.debug("Cleared all saved gamemodes");
    }
    
    /**
     * Verifica se um jogador tem gamemode salvo
     */
    public boolean hasOriginalGameMode(UUID playerUuid) {
        return originalGameModes.containsKey(playerUuid);
    }
    
    /**
     * Obtém gamemode original de um jogador
     */
    public GameType getOriginalGameMode(UUID playerUuid) {
        return originalGameModes.get(playerUuid);
    }
    
    /**
     * Encontra jogador online por UUID
     */
    private ServerPlayer findPlayerByUuid(UUID playerUuid) {
        // Esta é uma implementação simples - em um ambiente real você usaria
        // o MinecraftServer para encontrar o jogador
        return null; // Será implementado quando integrarmos com GameManager
    }
    
    /**
     * Define servidor para busca de jogadores
     */
    public void setServer(net.minecraft.server.MinecraftServer server) {
        // Implementação futura se necessário
    }
    
    /**
     * Obtém número de gamemodes salvos (para debug)
     */
    public int getSavedGameModesCount() {
        return originalGameModes.size();
    }
}