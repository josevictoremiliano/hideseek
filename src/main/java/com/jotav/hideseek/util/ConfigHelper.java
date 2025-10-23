package com.jotav.hideseek.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/**
 * Utilitário para conversão entre coordenadas e strings
 */
public class ConfigHelper {
    
    /**
     * Converte BlockPos e dimensão para string de configuração
     * Formato: "x,y,z,dimension"
     */
    public static String positionToString(BlockPos pos, ResourceKey<Level> dimension) {
        if (pos == null || dimension == null) {
            return "";
        }
        
        return String.format("%d,%d,%d,%s", 
            pos.getX(), pos.getY(), pos.getZ(), 
            dimension.location().toString());
    }
    
    /**
     * Converte string de configuração para BlockPos
     * Retorna null se inválida
     */
    public static BlockPos stringToPosition(String configString) {
        if (configString == null || configString.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = configString.split(",");
            if (parts.length >= 3) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                return new BlockPos(x, y, z);
            }
        } catch (NumberFormatException e) {
            // String inválida
        }
        
        return null;
    }
    
    /**
     * Extrai dimensão da string de configuração
     * Retorna null se inválida ou não especificada
     */
    public static ResourceKey<Level> stringToDimension(String configString, MinecraftServer server) {
        if (configString == null || configString.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = configString.split(",");
            if (parts.length >= 4) {
                String dimensionString = parts[3].trim();
                ResourceLocation dimensionLocation = ResourceLocation.parse(dimensionString);
                return ResourceKey.create(Registries.DIMENSION, dimensionLocation);
            }
        } catch (Exception e) {
            // String inválida
        }
        
        // Padrão: overworld
        return Level.OVERWORLD;
    }
    
    /**
     * Converte string de coordenadas simples (x,y,z) para BlockPos
     */
    public static BlockPos stringToSimplePosition(String configString) {
        if (configString == null || configString.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = configString.split(",");
            if (parts.length >= 3) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                return new BlockPos(x, y, z);
            }
        } catch (NumberFormatException e) {
            // String inválida
        }
        
        return null;
    }
    
    /**
     * Converte BlockPos para string simples (x,y,z)
     */
    public static String simplePositionToString(BlockPos pos) {
        if (pos == null) {
            return "";
        }
        
        return String.format("%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
    }
}