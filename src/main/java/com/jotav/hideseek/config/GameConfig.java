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
        
        // Configurações de tempo (em segundos)
        public int hideTime = 60;      // Tempo da fase HIDING
        public int seekTime = 300;     // Tempo da fase SEEKING
        public int startCountdown = 10; // Contagem regressiva antes do início
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
    
    // ================== MÉTODOS DE CONFIGURAÇÃO DE TEMPO ==================
    
    public boolean setHideTime(int seconds) {
        if (seconds >= 10 && seconds <= 600) {
            data.hideTime = seconds;
            saveConfig();
            HideSeek.LOGGER.info("Hide time set to {} seconds", seconds);
            return true;
        }
        return false;
    }
    
    public boolean setSeekTime(int seconds) {
        if (seconds >= 30 && seconds <= 1200) {
            data.seekTime = seconds;
            saveConfig();
            HideSeek.LOGGER.info("Seek time set to {} seconds", seconds);
            return true;
        }
        return false;
    }
    
    public boolean setStartCountdown(int seconds) {
        if (seconds >= 3 && seconds <= 30) {
            data.startCountdown = seconds;
            saveConfig();
            HideSeek.LOGGER.info("Start countdown set to {} seconds", seconds);
            return true;
        }
        return false;
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
    
    // ================== GETTERS PARA TEMPOS ==================
    
    public int getHideTime() {
        return data.hideTime;
    }
    
    public int getSeekTime() {
        return data.seekTime;
    }
    
    public int getStartCountdown() {
        return data.startCountdown;
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
    
    /**
     * Lista todas as configurações atuais
     */
    public String getAllConfigurations() {
        StringBuilder config = new StringBuilder();
        config.append("📍 Coordenadas:\n");
        config.append("  • Lobby: ").append(data.lobbySpawn.isEmpty() ? "Não definido" : data.lobbySpawn).append("\n");
        config.append("  • Seeker spawn: ").append(data.seekerSpawn.isEmpty() ? "Não definido" : data.seekerSpawn).append("\n");
        config.append("  • Map boundary: ").append(data.mapBoundaryMin.isEmpty() ? "Não definido" : 
            data.mapBoundaryMin + " to " + data.mapBoundaryMax).append("\n");
        
        config.append("\n⏰ Tempos de Jogo:\n");
        config.append("  • Tempo para esconder: ").append(data.hideTime).append(" segundos\n");
        config.append("  • Tempo para buscar: ").append(data.seekTime).append(" segundos\n");
        config.append("  • Contagem regressiva: ").append(data.startCountdown).append(" segundos\n");
        
        return config.toString();
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