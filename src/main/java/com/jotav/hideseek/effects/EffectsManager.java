package com.jotav.hideseek.effects;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.HideSeek;
import com.jotav.hideseek.config.GameConfig;
import com.jotav.hideseek.effects.GameModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Gerencia efeitos aplicados aos jogadores e teleportes seguros
 */
public class EffectsManager {
    private static EffectsManager instance;
    private final Set<ServerPlayer> playersWithEffects = new HashSet<>();
    private final GameModeManager gameModeManager = GameModeManager.getInstance();
    private final GameConfig gameConfig = GameConfig.getInstance();
    
    // Configuração de Jump Boost (+5 blocos = nível 4)
    private static final int JUMP_BOOST_LEVEL = 4; // +5 blocos de altura
    private static final int JUMP_BOOST_DURATION = Integer.MAX_VALUE; // Infinito
    
    // Configuração de Regeneração (para evitar fome e dano)
    private static final int REGENERATION_LEVEL = 1; // Nível 1 de regeneração
    private static final int REGENERATION_DURATION = Integer.MAX_VALUE; // Infinito durante jogo
    
    private EffectsManager() {}
    
    public static EffectsManager getInstance() {
        if (instance == null) {
            instance = new EffectsManager();
        }
        return instance;
    }
    
    /**
     * Aplica efeitos de imobilização aos Seekers durante a fase HIDING
     */
    public void applySeekerEffects(Set<ServerPlayer> seekers) {
        for (ServerPlayer seeker : seekers) {
            // Slowness 255 = imobilização quase total
            MobEffectInstance slowness = new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                Integer.MAX_VALUE, // Duração infinita (até ser removido)
                Config.SEEKER_SLOWNESS_LEVEL.get(),
                false, // Não mostrar partículas
                false, // Não mostrar no HUD
                false  // Não mostrar ícone
            );
            
            // Cegueira para impedir que vejam os Hiders se escondendo
            MobEffectInstance blindness = new MobEffectInstance(
                MobEffects.BLINDNESS,
                Integer.MAX_VALUE,
                Config.SEEKER_BLINDNESS_LEVEL.get(),
                false,
                false,
                false
            );
            
            // Jump Boost negativo para impedir pulos (nível -10)
            MobEffectInstance jumpBoost = new MobEffectInstance(
                MobEffects.JUMP,
                Integer.MAX_VALUE,
                -10, // Nível negativo para impedir pulos
                false,
                false,
                false
            );
            
            // Regeneração para evitar fome e dano
            MobEffectInstance regeneration = new MobEffectInstance(
                MobEffects.REGENERATION,
                REGENERATION_DURATION,
                REGENERATION_LEVEL,
                false, false, false
            );
            
            seeker.addEffect(slowness);
            seeker.addEffect(blindness);
            seeker.addEffect(jumpBoost);
            seeker.addEffect(regeneration);
            
            // Mudar para Adventure Mode para impedir quebra de blocos
            gameModeManager.setGameModeToAdventure(seeker);
            
            playersWithEffects.add(seeker);
            
            HideSeek.LOGGER.debug("Applied seeker effects to player: {}", seeker.getName().getString());
        }
    }
    
    /**
     * Aplica Adventure Mode e Jump Boost temporário para Hiders durante HIDING
     */
    public void applyHiderEffects(Set<ServerPlayer> hiders) {
        for (ServerPlayer hider : hiders) {
            // Mudar para Adventure Mode para impedir quebra de blocos
            gameModeManager.setGameModeToAdventure(hider);
            
            // Aplicar Jump Boost temporário (+5 blocos)
            MobEffectInstance jumpBoost = new MobEffectInstance(
                MobEffects.JUMP,
                JUMP_BOOST_DURATION,
                JUMP_BOOST_LEVEL,
                false, false, false
            );
            
            // Regeneração para evitar fome e dano
            MobEffectInstance regeneration = new MobEffectInstance(
                MobEffects.REGENERATION,
                REGENERATION_DURATION,
                REGENERATION_LEVEL,
                false, false, false
            );
            
            hider.addEffect(jumpBoost);
            hider.addEffect(regeneration);
            
            playersWithEffects.add(hider);
            HideSeek.LOGGER.debug("Applied adventure mode, jump boost and regeneration to hider: {}", hider.getName().getString());
        }
    }
    
    /**
     * Remove Jump Boost dos Hiders (quando fase SEEKING começa)
     */
    public void removeHiderJumpBoost(Set<ServerPlayer> hiders) {
        for (ServerPlayer hider : hiders) {
            // Remover Jump Boost dos Hiders
            hider.removeEffect(MobEffects.JUMP);
            
            HideSeek.LOGGER.debug("Removed jump boost from hider: {}", hider.getName().getString());
        }
    }
    
    /**
     * Aplica Adventure Mode para espectadores
     */
    public void applySpectatorEffects(Set<ServerPlayer> spectators) {
        for (ServerPlayer spectator : spectators) {
            // Mudar para Adventure Mode
            gameModeManager.setGameModeToAdventure(spectator);
            
            // Regeneração para evitar fome e dano
            MobEffectInstance regeneration = new MobEffectInstance(
                MobEffects.REGENERATION,
                REGENERATION_DURATION,
                REGENERATION_LEVEL,
                false, false, false
            );
            spectator.addEffect(regeneration);
            
            playersWithEffects.add(spectator);
            HideSeek.LOGGER.debug("Applied adventure mode and regeneration to spectator: {}", spectator.getName().getString());
        }
    }
    
    /**
     * Remove todos os efeitos e restaura gamemodes para todos os jogadores
     */
    public void clearAllEffectsAndRestoreGameModes() {
        for (ServerPlayer player : new HashSet<>(playersWithEffects)) {
            // Remover todos os efeitos de poção
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.JUMP);
            player.removeEffect(MobEffects.REGENERATION);
            
            // Restaurar gamemode original (Survival)
            gameModeManager.restoreOriginalGameMode(player);
        }
        
        // Limpar set de jogadores com efeitos
        playersWithEffects.clear();
        
        // Restaurar todos os gamemodes
        gameModeManager.restoreAllGameModes();
        
        HideSeek.LOGGER.info("Cleared all effects and restored gamemodes for all players");
    }
    
    /**
     * Remove todos os efeitos de imobilização dos Seekers e aplica Jump Boost permanente
     */
    public void removeSeekerEffects(Set<ServerPlayer> seekers) {
        for (ServerPlayer seeker : seekers) {
            seeker.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            seeker.removeEffect(MobEffects.BLINDNESS);
            seeker.removeEffect(MobEffects.JUMP);
            
            // Aplicar Jump Boost permanente para Seekers (+5 blocos)
            MobEffectInstance jumpBoost = new MobEffectInstance(
                MobEffects.JUMP,
                JUMP_BOOST_DURATION,
                JUMP_BOOST_LEVEL,
                false, false, false
            );
            seeker.addEffect(jumpBoost);
            
            // NÃO remover do playersWithEffects - eles ainda têm regeneração ativa
            
            HideSeek.LOGGER.debug("Removed seeker immobilization effects and applied permanent jump boost to: {}", seeker.getName().getString());
        }
    }
    
    /**
     * Remove todos os efeitos de todos os jogadores (reset completo)
     */
    public void clearAllEffects() {
        for (ServerPlayer player : playersWithEffects) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.JUMP);
        }
        
        playersWithEffects.clear();
        HideSeek.LOGGER.debug("Cleared all player effects");
    }
    
    /**
     * Teleporta jogador de forma segura para uma posição
     */
    public boolean safeTeleport(ServerPlayer player, BlockPos targetPos, ResourceKey<Level> dimension) {
        if (player == null || targetPos == null) {
            return false;
        }
        
        try {
            ServerLevel targetLevel = player.getServer().getLevel(dimension);
            if (targetLevel == null) {
                HideSeek.LOGGER.warn("Target dimension not found: {}", dimension.location());
                return false;
            }
            
            // Verificar se a posição é segura (não dentro de blocos sólidos)
            BlockPos safePos = findSafePosition(targetLevel, targetPos);
            if (safePos == null) {
                HideSeek.LOGGER.warn("No safe position found near: {}", targetPos);
                return false;
            }
            
            // Teleportar
            player.teleportTo(
                targetLevel,
                safePos.getX() + 0.5, // Centro do bloco
                safePos.getY(),
                safePos.getZ() + 0.5,
                player.getYRot(), // Manter rotação atual
                player.getXRot()
            );
            
            HideSeek.LOGGER.debug("Teleported {} to {}", player.getName().getString(), safePos);
            return true;
            
        } catch (Exception e) {
            HideSeek.LOGGER.error("Failed to teleport player {}: {}", player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Encontra uma posição segura próxima à posição alvo
     */
    private BlockPos findSafePosition(ServerLevel level, BlockPos targetPos) {
        // Primeiro, tentar a posição exata
        if (isSafePosition(level, targetPos)) {
            return targetPos;
        }
        
        // Procurar em um raio de 5 blocos
        for (int radius = 1; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 3; y++) { // Verificar alguns blocos acima e abaixo
                        BlockPos testPos = targetPos.offset(x, y, z);
                        if (isSafePosition(level, testPos)) {
                            return testPos;
                        }
                    }
                }
            }
        }
        
        // Se não encontrou posição segura, retornar a original (melhor que nada)
        return targetPos;
    }
    
    /**
     * Verifica se uma posição é segura para teleporte
     */
    private boolean isSafePosition(ServerLevel level, BlockPos pos) {
        try {
            // Verificar se há espaço para o jogador (2 blocos de altura)
            return !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos) &&
                   !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above()) &&
                   level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Teleporta jogador para o lobby
     */
    public boolean teleportToLobby(ServerPlayer player) {
        BlockPos lobbyPos = gameConfig.getLobbySpawn();
        if (lobbyPos == null) {
            HideSeek.LOGGER.warn("Lobby spawn not set, cannot teleport player: {}", player.getName().getString());
            return false;
        }
        
        // Assumir Overworld se não especificado
        return safeTeleport(player, lobbyPos, Level.OVERWORLD);
    }
    
    /**
     * Teleporta Seekers para o spawn específico deles
     */
    public boolean teleportSeekersToSpawn(Set<ServerPlayer> seekers) {
        BlockPos seekerSpawn = gameConfig.getSeekerSpawn();
        if (seekerSpawn == null) {
            HideSeek.LOGGER.warn("Seeker spawn not set, cannot teleport seekers");
            return false;
        }
        
        HideSeek.LOGGER.info("Teleporting {} seekers to spawn: {}", seekers.size(), seekerSpawn);
        
        boolean allSuccess = true;
        for (ServerPlayer seeker : seekers) {
            HideSeek.LOGGER.info("Teleporting seeker {} from {} to {}", 
                seeker.getName().getString(), seeker.blockPosition(), seekerSpawn);
            
            if (!safeTeleport(seeker, seekerSpawn, Level.OVERWORLD)) {
                HideSeek.LOGGER.error("Failed to teleport seeker: {}", seeker.getName().getString());
                allSuccess = false;
            } else {
                HideSeek.LOGGER.info("Successfully teleported seeker: {}", seeker.getName().getString());
            }
        }
        
        return allSuccess;
    }
    
    /**
     * Verifica se um jogador está dentro dos limites do mapa
     */
    public boolean isPlayerInBounds(ServerPlayer player) {
        BlockPos minBounds = gameConfig.getMapBoundaryMin();
        BlockPos maxBounds = gameConfig.getMapBoundaryMax();
        
        if (minBounds == null || maxBounds == null) {
            return true; // Se não há limites definidos, considerar sempre válido
        }
        
        BlockPos playerPos = player.blockPosition();
        
        return playerPos.getX() >= minBounds.getX() && playerPos.getX() <= maxBounds.getX() &&
               playerPos.getY() >= minBounds.getY() && playerPos.getY() <= maxBounds.getY() &&
               playerPos.getZ() >= minBounds.getZ() && playerPos.getZ() <= maxBounds.getZ();
    }
    
    /**
     * Teleporta jogador de volta se estiver fora dos limites
     */
    public void enforceMapBoundaries(ServerPlayer player) {
        if (!isPlayerInBounds(player)) {
            // Tentar teleportar de volta ao lobby
            if (!teleportToLobby(player)) {
                // Se falhou, teleportar para spawn do mundo
                BlockPos worldSpawn = player.serverLevel().getSharedSpawnPos();
                safeTeleport(player, worldSpawn, player.level().dimension());
            }
            
            // TODO: Enviar mensagem ao jogador sobre os limites
            HideSeek.LOGGER.info("Player {} was outside map boundaries and was teleported back", 
                                player.getName().getString());
        }
    }
}