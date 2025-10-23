package com.jotav.hideseek.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import java.util.*;

/**
 * Gerencia jogadores, times e estado do jogo
 */
public class PlayerManager {
    private final Set<ServerPlayer> lobbyPlayers = new HashSet<>();
    private final Set<ServerPlayer> hiders = new HashSet<>();
    private final Set<ServerPlayer> seekers = new HashSet<>();
    private final Set<ServerPlayer> spectators = new HashSet<>();
    
    /**
     * Adiciona jogador ao lobby
     */
    public boolean joinLobby(ServerPlayer player) {
        if (isPlayerInGame(player)) {
            return false;
        }
        return lobbyPlayers.add(player);
    }
    
    /**
     * Remove jogador do jogo completamente
     */
    public boolean leaveGame(ServerPlayer player) {
        boolean removed = lobbyPlayers.remove(player) ||
                         hiders.remove(player) ||
                         seekers.remove(player) ||
                         spectators.remove(player);
        return removed;
    }
    
    /**
     * Verifica se jogador está em qualquer estado do jogo
     */
    public boolean isPlayerInGame(ServerPlayer player) {
        return lobbyPlayers.contains(player) ||
               hiders.contains(player) ||
               seekers.contains(player) ||
               spectators.contains(player);
    }
    
    /**
     * Distribui jogadores do lobby para times
     */
    public void assignTeams(int minHiders, int maxHiders) {
        List<ServerPlayer> players = new ArrayList<>(lobbyPlayers);
        Collections.shuffle(players);
        
        int totalPlayers = players.size();
        int hidersCount = Math.max(minHiders, Math.min(maxHiders, totalPlayers / 2));
        
        hiders.clear();
        seekers.clear();
        
        for (int i = 0; i < hidersCount && i < players.size(); i++) {
            hiders.add(players.get(i));
        }
        
        for (int i = hidersCount; i < players.size(); i++) {
            seekers.add(players.get(i));
        }
        
        lobbyPlayers.clear();
    }
    
    /**
     * Move hider capturado para espectadores
     */
    public boolean captureHider(ServerPlayer hider) {
        if (hiders.remove(hider)) {
            spectators.add(hider);
            
            // Aplicar Adventure Mode para o espectador
            com.jotav.hideseek.effects.EffectsManager.getInstance().applySpectatorEffects(Set.of(hider));
            
            return true;
        }
        return false;
    }
    
    /**
     * Reset completo - todos para lobby
     */
    public void resetAll() {
        Set<ServerPlayer> allPlayers = new HashSet<>();
        allPlayers.addAll(lobbyPlayers);
        allPlayers.addAll(hiders);
        allPlayers.addAll(seekers);
        allPlayers.addAll(spectators);
        
        lobbyPlayers.clear();
        hiders.clear();
        seekers.clear();
        spectators.clear();
        
        lobbyPlayers.addAll(allPlayers);
    }
    
    /**
     * Remove todos os jogadores completamente (para leaveall)
     */
    public Set<ServerPlayer> removeAllPlayers() {
        Set<ServerPlayer> allPlayers = new HashSet<>();
        allPlayers.addAll(lobbyPlayers);
        allPlayers.addAll(hiders);
        allPlayers.addAll(seekers);
        allPlayers.addAll(spectators);
        
        lobbyPlayers.clear();
        hiders.clear();
        seekers.clear();
        spectators.clear();
        
        return allPlayers;
    }
    
    // Getters
    public Set<ServerPlayer> getLobbyPlayers() { return new HashSet<>(lobbyPlayers); }
    public Set<ServerPlayer> getHiders() { return new HashSet<>(hiders); }
    public Set<ServerPlayer> getSeekers() { return new HashSet<>(seekers); }
    public Set<ServerPlayer> getSpectators() { return new HashSet<>(spectators); }
    
    public int getLobbyCount() { return lobbyPlayers.size(); }
    public int getHidersCount() { return hiders.size(); }
    public int getSeekersCount() { return seekers.size(); }
    public int getTotalPlayerCount() { 
        return lobbyPlayers.size() + hiders.size() + seekers.size() + spectators.size();
    }
    public int getTotalPlayers() { 
        return lobbyPlayers.size() + hiders.size() + seekers.size() + spectators.size(); 
    }
}