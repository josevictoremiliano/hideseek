package com.jotav.hideseek.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;

/**
 * Gerencia mensagens de chat para eventos do jogo Hide and Seek
 */
public class ChatManager {
    private static ChatManager instance;
    
    // Prefixos para diferentes tipos de mensagens
    private static final Component PREFIX_INFO = Component.literal("[HnS] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    private static final Component PREFIX_SUCCESS = Component.literal("[HnS] ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    private static final Component PREFIX_WARNING = Component.literal("[HnS] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
    private static final Component PREFIX_ERROR = Component.literal("[HnS] ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    private static final Component PREFIX_GAME = Component.literal("[HnS] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    
    private ChatManager() {}
    
    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }
    
    // =================== MÉTODOS DE BROADCAST ===================
    
    /**
     * Envia mensagem para todos os jogadores do servidor
     */
    public void broadcastToAll(MinecraftServer server, Component message) {
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        }
    }
    
    /**
     * Envia mensagem para uma coleção específica de jogadores
     */
    public void sendToPlayers(Collection<ServerPlayer> players, Component message) {
        for (ServerPlayer player : players) {
            player.sendSystemMessage(message);
        }
    }
    
    /**
     * Envia mensagem para um jogador específico
     */
    public void sendToPlayer(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }
    
    // =================== MENSAGENS DE ENTRADA/SAÍDA ===================
    
    public void playerJoinedGame(MinecraftServer server, ServerPlayer player, int totalPlayers, int minRequired) {
        Component message = PREFIX_SUCCESS
            .copy()
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" entrou no jogo! (").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(totalPlayers)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(minRequired)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" jogadores)").withStyle(ChatFormatting.GRAY));
        
        broadcastToAll(server, message);
    }
    
    public void playerLeftGame(MinecraftServer server, ServerPlayer player, int remainingPlayers) {
        Component message = PREFIX_WARNING
            .copy()
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" saiu do jogo. Restam ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(remainingPlayers)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" jogadores.").withStyle(ChatFormatting.GRAY));
        
        broadcastToAll(server, message);
    }
    
    // =================== MENSAGENS DE ESTADO DO JOGO ===================
    
    public void gameStartingCountdown(MinecraftServer server, int seconds) {
        Component message = PREFIX_GAME
            .copy()
            .append(Component.literal("🚀 Jogo iniciando em ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos!").withStyle(ChatFormatting.YELLOW));
        
        broadcastToAll(server, message);
    }
    
    public void teamsAssigned(MinecraftServer server, int hidersCount, int seekersCount) {
        Component message = PREFIX_GAME
            .copy()
            .append(Component.literal("Times formados! ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(String.valueOf(hidersCount)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal(" Hiders").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" × ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(seekersCount)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(" Seekers").withStyle(ChatFormatting.RED));
        
        broadcastToAll(server, message);
    }
    
    public void hidingPhaseStarted(MinecraftServer server, int hideTimeSeconds) {
        // Mensagem para Hiders
        Component hidersMessage = PREFIX_SUCCESS
            .copy()
            .append(Component.literal("👁 Fase de ESCONDER iniciada! Vocês têm ").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(String.valueOf(hideTimeSeconds)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos para se esconder!").withStyle(ChatFormatting.GREEN));
        
        // Mensagem para Seekers
        Component seekersMessage = PREFIX_INFO
            .copy()
            .append(Component.literal("⏳ Aguardem! Vocês serão liberados em ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(hideTimeSeconds)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos.").withStyle(ChatFormatting.YELLOW));
        
        broadcastToAll(server, hidersMessage);
        broadcastToAll(server, seekersMessage);
    }
    
    public void seekingPhaseStarted(MinecraftServer server, int seekTimeSeconds, int hidersRemaining) {
        Component message = PREFIX_ERROR
            .copy()
            .append(Component.literal("🔍 SEEKERS LIBERADOS! Encontrem os ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(String.valueOf(hidersRemaining)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" Hiders em ").withStyle(ChatFormatting.RED))
            .append(Component.literal(String.valueOf(seekTimeSeconds)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos!").withStyle(ChatFormatting.RED));
        
        broadcastToAll(server, message);
    }
    
    // =================== MENSAGENS DE CAPTURA ===================
    
    public void playerCaptured(MinecraftServer server, ServerPlayer hider, ServerPlayer seeker, int hidersRemaining) {
        Component message = PREFIX_ERROR
            .copy()
            .append(Component.literal("💀 ").withStyle(ChatFormatting.RED))
            .append(Component.literal(hider.getName().getString()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" foi capturado por ").withStyle(ChatFormatting.RED))
            .append(Component.literal(seeker.getName().getString()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal("! Restam ").withStyle(ChatFormatting.RED))
            .append(Component.literal(String.valueOf(hidersRemaining)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" Hiders.").withStyle(ChatFormatting.RED));
        
        broadcastToAll(server, message);
        
        // Mensagem personalizada para o Hider capturado
        Component capturedMessage = PREFIX_INFO
            .copy()
            .append(Component.literal("Você foi capturado! Agora é um espectador.").withStyle(ChatFormatting.GRAY));
        
        sendToPlayer(hider, capturedMessage);
    }
    
    // =================== MENSAGENS DE VITÓRIA ===================
    
    public void seekersWin(MinecraftServer server, Set<ServerPlayer> seekers) {
        Component message = PREFIX_SUCCESS
            .copy()
            .append(Component.literal("🏆 SEEKERS VENCERAM! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("Todos os Hiders foram capturados!").withStyle(ChatFormatting.YELLOW));
        
        broadcastToAll(server, message);
        
        // Parabenizar Seekers individualmente
        for (ServerPlayer seeker : seekers) {
            Component congratsMessage = PREFIX_SUCCESS
                .copy()
                .append(Component.literal("🎉 Parabéns, ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(seeker.getName().getString()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("! Você ajudou a capturar todos os Hiders!").withStyle(ChatFormatting.GREEN));
            
            sendToPlayer(seeker, congratsMessage);
        }
    }
    
    public void hidersWin(MinecraftServer server, Set<ServerPlayer> hiders, int survivorsCount) {
        Component message = PREFIX_SUCCESS
            .copy()
            .append(Component.literal("🏆 HIDERS VENCERAM! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(String.valueOf(survivorsCount)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" Hider(s) sobreviveram!").withStyle(ChatFormatting.GREEN));
        
        broadcastToAll(server, message);
        
        // Parabenizar Hiders sobreviventes
        for (ServerPlayer hider : hiders) {
            Component congratsMessage = PREFIX_SUCCESS
                .copy()
                .append(Component.literal("🎉 Parabéns, ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(hider.getName().getString()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("! Você conseguiu se esconder até o final!").withStyle(ChatFormatting.GREEN));
            
            sendToPlayer(hider, congratsMessage);
        }
    }
    
    // =================== MENSAGENS DE TEMPO ===================
    
    public void timeWarning(MinecraftServer server, int secondsRemaining, String phase) {
        ChatFormatting color = secondsRemaining <= 10 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        
        Component message = PREFIX_WARNING
            .copy()
            .append(Component.literal("⚠️ ").withStyle(color))
            .append(Component.literal(String.valueOf(secondsRemaining)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos restantes na fase ").withStyle(color))
            .append(Component.literal(phase).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal("!").withStyle(color));
        
        broadcastToAll(server, message);
    }
    
    // =================== MENSAGENS DE ERRO/INFO ===================
    
    public void gameAlreadyInProgress(ServerPlayer player) {
        Component message = PREFIX_ERROR
            .copy()
            .append(Component.literal("O jogo já está em andamento! Aguarde terminar.").withStyle(ChatFormatting.RED));
        
        sendToPlayer(player, message);
    }
    
    public void notEnoughPlayers(ServerPlayer player, int current, int required) {
        Component message = PREFIX_ERROR
            .copy()
            .append(Component.literal("Não há jogadores suficientes! (").withStyle(ChatFormatting.RED))
            .append(Component.literal(String.valueOf(current)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(required)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(")").withStyle(ChatFormatting.RED));
        
        sendToPlayer(player, message);
    }
    
    public void gameReset(MinecraftServer server) {
        Component message = PREFIX_WARNING
            .copy()
            .append(Component.literal("🔄 Jogo resetado por um administrador.").withStyle(ChatFormatting.YELLOW));
        
        broadcastToAll(server, message);
    }
    
    public void returningToLobby(MinecraftServer server, int seconds) {
        Component message = PREFIX_INFO
            .copy()
            .append(Component.literal("↩️ Retornando ao lobby em ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" segundos...").withStyle(ChatFormatting.GRAY));
        
        broadcastToAll(server, message);
    }
}