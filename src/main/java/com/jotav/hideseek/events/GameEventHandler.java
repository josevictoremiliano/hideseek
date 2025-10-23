package com.jotav.hideseek.events;

import com.jotav.hideseek.game.GameManager;
import com.jotav.hideseek.game.GameState;
import com.jotav.hideseek.effects.EffectsManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handlers para o jogo Hide and Seek
 */
@EventBusSubscriber(modid = "hideseek")
public class GameEventHandler {
    
    /**
     * Detecta quando um jogador ataca outro durante o jogo
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        
        GameManager gameManager = GameManager.getInstance();
        
        // Só processar durante a fase SEEKING
        if (gameManager.getCurrentState() != GameState.SEEKING) {
            return;
        }
        
        // Verificar se attacker é Seeker e target é Hider
        boolean attackerIsSeeker = gameManager.getPlayerManager().getSeekers().contains(attacker);
        boolean targetIsHider = gameManager.getPlayerManager().getHiders().contains(target);
        
        if (attackerIsSeeker && targetIsHider) {
            // Capturar o Hider
            gameManager.captureHider(target, attacker);
            
            // Cancelar o ataque para não causar dano
            event.setCanceled(true);
            
            com.jotav.hideseek.HideSeek.LOGGER.info("Hider {} captured by Seeker {}", 
                                                   target.getName().getString(), 
                                                   attacker.getName().getString());
        } else if (!attackerIsSeeker || !gameManager.getPlayerManager().isPlayerInGame(attacker)) {
            // Cancelar ataques entre jogadores que não estão no jogo ou não são da combinação correta
            event.setCanceled(true);
        }
    }
    
    /**
     * Verifica limites do mapa a cada tick para jogadores ativos
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        GameManager gameManager = GameManager.getInstance();
        GameState currentState = gameManager.getCurrentState();
        
        // Só verificar durante fases ativas do jogo
        if (currentState != GameState.HIDING && currentState != GameState.SEEKING) {
            return;
        }
        
        // Só verificar jogadores que estão no jogo
        if (!gameManager.getPlayerManager().isPlayerInGame(player)) {
            return;
        }
        
        // Verificar limites do mapa a cada 20 ticks (1 segundo)
        if (player.tickCount % 20 == 0) {
            EffectsManager.getInstance().enforceMapBoundaries(player);
        }
    }
}