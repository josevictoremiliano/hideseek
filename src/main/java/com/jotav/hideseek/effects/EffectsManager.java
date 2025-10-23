package com.jotav.hideseek.effects;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.HideSeek;
import com.jotav.hideseek.game.GameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
            
            seeker.addEffect(slowness);
            seeker.addEffect(blindness);
            seeker.addEffect(jumpBoost);
            
            playersWithEffects.add(seeker);
            
            HideSeek.LOGGER.debug("Applied seeker effects to player: {}", seeker.getName().getString());
        }
    }
    
    /**
     * Remove todos os efeitos de imobilização dos Seekers
     */
    public void removeSeekerEffects(Set<ServerPlayer> seekers) {
        for (ServerPlayer seeker : seekers) {
            seeker.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            seeker.removeEffect(MobEffects.BLINDNESS);
            seeker.removeEffect(MobEffects.JUMP);
            
            playersWithEffects.remove(seeker);
            
            HideSeek.LOGGER.debug("Removed seeker effects from player: {}", seeker.getName().getString());
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
        BlockPos lobbyPos = GameManager.getInstance().getLobbySpawn();
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
        BlockPos seekerSpawn = GameManager.getInstance().getSeekerSpawn();
        if (seekerSpawn == null) {
            HideSeek.LOGGER.warn("Seeker spawn not set, cannot teleport seekers");
            return false;
        }
        
        boolean allSuccess = true;
        for (ServerPlayer seeker : seekers) {
            if (!safeTeleport(seeker, seekerSpawn, Level.OVERWORLD)) {
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    /**
     * Verifica se um jogador está dentro dos limites do mapa
     */
    public boolean isPlayerInBounds(ServerPlayer player) {
        BlockPos minBounds = GameManager.getInstance().getMapBoundaryMin();
        BlockPos maxBounds = GameManager.getInstance().getMapBoundaryMax();
        
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