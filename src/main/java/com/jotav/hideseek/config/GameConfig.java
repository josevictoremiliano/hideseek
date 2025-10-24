package com.jotav.hideseek.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jotav.hideseek.HideSeek;
import com.jotav.hideseek.util.ConfigHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configurações persistentes do jogo Hide and Seek
 */
public class GameConfig {
    private static GameConfig instance;
    private final Path configFile;
    private final Gson gson;
    
    // Dados de configuração
    private ConfigData data = new ConfigData();
    
    private GameConfig() {
        this.configFile = Paths.get("hideseek_config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadConfig();
    }
    
    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }
    
    /**
     * Classe para dados de configuração
     */
    private static class ConfigData {
        public String lobbySpawn = "";
        public String seekerSpawn = "";
        public String mapBoundaryMin = "";
        public String mapBoundaryMax = "";
    }
    
    // ================== MÉTODOS DE CONFIGURAÇÃO ==================
    
    public void setLobbySpawn(BlockPos pos, ResourceKey<Level> dimension) {
        data.lobbySpawn = ConfigHelper.positionToString(pos, dimension);
        saveConfig();
        HideSeek.LOGGER.info("Lobby spawn saved: {}", data.lobbySpawn);
    }
    
    public void setSeekerSpawn(BlockPos pos, ResourceKey<Level> dimension) {
        data.seekerSpawn = ConfigHelper.positionToString(pos, dimension);
        saveConfig();
        HideSeek.LOGGER.info("Seeker spawn saved: {}", data.seekerSpawn);
    }
    
    public void setMapBoundary(BlockPos min, BlockPos max) {
        data.mapBoundaryMin = ConfigHelper.simplePositionToString(min);
        data.mapBoundaryMax = ConfigHelper.simplePositionToString(max);
        saveConfig();
        HideSeek.LOGGER.info("Map boundary saved: {} to {}", data.mapBoundaryMin, data.mapBoundaryMax);
    }
    
    // ================== MÉTODOS DE OBTENÇÃO ==================
    
    public BlockPos getLobbySpawn() {
        return ConfigHelper.stringToPosition(data.lobbySpawn);
    }
    
    public String getLobbySpawnString() {
        return data.lobbySpawn;
    }
    
    public BlockPos getSeekerSpawn() {
        return ConfigHelper.stringToPosition(data.seekerSpawn);
    }
    
    public String getSeekerSpawnString() {
        return data.seekerSpawn;
    }
    
    public BlockPos getMapBoundaryMin() {
        return ConfigHelper.stringToSimplePosition(data.mapBoundaryMin);
    }
    
    public BlockPos getMapBoundaryMax() {
        return ConfigHelper.stringToSimplePosition(data.mapBoundaryMax);
    }
    
    public String getMapBoundaryMinString() {
        return data.mapBoundaryMin;
    }
    
    public String getMapBoundaryMaxString() {
        return data.mapBoundaryMax;
    }
    
    /**
     * Verifica se todas as configurações essenciais estão definidas
     */
    public boolean isFullyConfigured() {
        return !data.lobbySpawn.isEmpty() && 
               !data.seekerSpawn.isEmpty() && 
               !data.mapBoundaryMin.isEmpty() && 
               !data.mapBoundaryMax.isEmpty();
    }
    
    /**
     * Lista configurações faltando
     */
    public String getMissingConfigurations() {
        StringBuilder missing = new StringBuilder();
        
        if (data.lobbySpawn.isEmpty()) {
            missing.append("- Lobby spawn (/hns set lobby)\n");
        }
        if (data.seekerSpawn.isEmpty()) {
            missing.append("- Seeker spawn (/hns set seekerspawn)\n");
        }
        if (data.mapBoundaryMin.isEmpty() || data.mapBoundaryMax.isEmpty()) {
            missing.append("- Map boundary (/hns set mapboundary <pos1> <pos2>)\n");
        }
        
        return missing.toString();
    }
    
    // ================== PERSISTÊNCIA ==================
    
    private void saveConfig() {
        try {
            String json = gson.toJson(data);
            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                writer.write(json);
            }
            HideSeek.LOGGER.debug("Game config saved");
        } catch (IOException e) {
            HideSeek.LOGGER.error("Failed to save game config", e);
        }
    }
    
    private void loadConfig() {
        if (!Files.exists(configFile)) {
            HideSeek.LOGGER.info("Game config file not found, starting with defaults");
            saveConfig(); // Criar arquivo padrão
            return;
        }
        
        try {
            String json = Files.readString(configFile);
            ConfigData loadedData = gson.fromJson(json, ConfigData.class);
            
            if (loadedData != null) {
                this.data = loadedData;
                HideSeek.LOGGER.info("Game config loaded successfully");
            }
        } catch (IOException e) {
            HideSeek.LOGGER.error("Failed to load game config", e);
        } catch (Exception e) {
            HideSeek.LOGGER.error("Failed to parse game config JSON", e);
        }
    }
}