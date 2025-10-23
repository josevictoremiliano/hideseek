package com.jotav.hideseek.ui;

import com.jotav.hideseek.Config;
import com.jotav.hideseek.chat.ChatManager;
import com.jotav.hideseek.game.GameManager;
import com.jotav.hideseek.game.GameState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.ChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Gerencia BossBar para mostrar contagem regressiva e status do jogo
 */
public class BossBarManager {
    private static BossBarManager instance;
    private final Map<GameState, CustomBossEvent> bossBars = new HashMap<>();
    private MinecraftServer server;
    private Timer updateTimer;
    private long phaseStartTime;
    private int phaseDurationSeconds;
    
    private BossBarManager() {
        initializeBossBars();
    }
    
    public static BossBarManager getInstance() {
        if (instance == null) {
            instance = new BossBarManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
        
        // Limpar estado anterior para evitar conflitos
        stopTimer();
        bossBars.clear();
        
        // Reinicializar BossBars
        initializeBossBars();
    }
    
    private void initializeBossBars() {
        // BossBar para fase STARTING (contagem regressiva)
        CustomBossEvent startingBar = new CustomBossEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hideseek", "starting"),
            Component.literal("Iniciando jogo...").withStyle(ChatFormatting.YELLOW)
        );
        startingBar.setColor(BossEvent.BossBarColor.YELLOW);
        startingBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
        bossBars.put(GameState.STARTING, startingBar);
        
        // BossBar para fase HIDING
        CustomBossEvent hidingBar = new CustomBossEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hideseek", "hiding"),
            Component.literal("Fase de Esconder").withStyle(ChatFormatting.GREEN)
        );
        hidingBar.setColor(BossEvent.BossBarColor.GREEN);
        hidingBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        bossBars.put(GameState.HIDING, hidingBar);
        
        // BossBar para fase SEEKING
        CustomBossEvent seekingBar = new CustomBossEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hideseek", "seeking"),
            Component.literal("Fase de Busca").withStyle(ChatFormatting.RED)
        );
        seekingBar.setColor(BossEvent.BossBarColor.RED);
        seekingBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        bossBars.put(GameState.SEEKING, seekingBar);
        
        // BossBar para fase ENDING
        CustomBossEvent endingBar = new CustomBossEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hideseek", "ending"),
            Component.literal("Jogo Terminado").withStyle(ChatFormatting.GOLD)
        );
        endingBar.setColor(BossEvent.BossBarColor.PURPLE);
        endingBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        bossBars.put(GameState.ENDING, endingBar);
    }
    
    /**
     * Inicia timer para uma fase específica
     */
    public void startPhaseTimer(GameState phase, int durationSeconds) {
        stopTimer(); // Para timer anterior se houver
        resetTimeWarnings(); // Resetar avisos de tempo
        
        this.phaseStartTime = System.currentTimeMillis();
        this.phaseDurationSeconds = durationSeconds;
        
        CustomBossEvent bossBar = bossBars.get(phase);
        if (bossBar == null) return;
        
        // Adicionar todos os jogadores ao BossBar
        GameManager gameManager = GameManager.getInstance();
        gameManager.getPlayerManager().getLobbyPlayers().forEach(bossBar::addPlayer);
        gameManager.getPlayerManager().getHiders().forEach(bossBar::addPlayer);
        gameManager.getPlayerManager().getSeekers().forEach(bossBar::addPlayer);
        gameManager.getPlayerManager().getSpectators().forEach(bossBar::addPlayer);
        
        // Iniciar timer de atualização
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateBossBar(phase, bossBar);
            }
        }, 0, 1000); // Atualizar a cada segundo
    }
    
    /**
     * Atualiza o BossBar com tempo restante
     */
    private void updateBossBar(GameState phase, CustomBossEvent bossBar) {
        long elapsedMs = System.currentTimeMillis() - phaseStartTime;
        int elapsedSeconds = (int) (elapsedMs / 1000);
        int remainingSeconds = Math.max(0, phaseDurationSeconds - elapsedSeconds);
        
        // Enviar avisos de tempo
        sendTimeWarnings(phase, remainingSeconds);
        
        // Calcular progresso (1.0 = cheio, 0.0 = vazio)
        float progress = (float) remainingSeconds / phaseDurationSeconds;
        bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
        
        // Atualizar texto baseado na fase
        Component title = switch (phase) {
            case STARTING -> Component.literal(
                String.format("Jogo iniciando em %ds", remainingSeconds)
            ).withStyle(ChatFormatting.YELLOW);
            
            case HIDING -> {
                int hidersCount = GameManager.getInstance().getPlayerManager().getHidersCount();
                yield Component.literal(
                    String.format("Escondendo... %ds restantes (%d Hiders)", remainingSeconds, hidersCount)
                ).withStyle(ChatFormatting.GREEN);
            }
            
            case SEEKING -> {
                int hidersCount = GameManager.getInstance().getPlayerManager().getHidersCount();
                yield Component.literal(
                    String.format("Buscando... %ds restantes (%d Hiders)", remainingSeconds, hidersCount)
                ).withStyle(ChatFormatting.RED);
            }
            
            case ENDING -> Component.literal("Retornando ao lobby...")
                .withStyle(ChatFormatting.GOLD);
            
            default -> Component.literal("Hide and Seek");
        };
        
        bossBar.setName(title);
        
        // Mudar cor conforme tempo restante
        if (remainingSeconds <= 10 && phase != GameState.ENDING) {
            bossBar.setColor(BossEvent.BossBarColor.RED);
        }
    }
    
    /**
     * Para todos os timers e remove BossBars
     */
    public void stopTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        
        // Remover todos os jogadores de todos os BossBars
        for (CustomBossEvent bossBar : bossBars.values()) {
            bossBar.removeAllPlayers();
        }
    }
    
    /**
     * Adiciona jogador aos BossBars ativos
     */
    public void addPlayer(ServerPlayer player) {
        GameState currentState = GameManager.getInstance().getCurrentState();
        CustomBossEvent bossBar = bossBars.get(currentState);
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }
    
    /**
     * Remove jogador de todos os BossBars
     */
    public void removePlayer(ServerPlayer player) {
        for (CustomBossEvent bossBar : bossBars.values()) {
            bossBar.removePlayer(player);
        }
    }
    
    /**
     * Mostra BossBar de vitória/derrota
     */
    public void showGameResult(boolean seekersWin) {
        stopTimer(); // Para timers de fase
        
        Component message = seekersWin ? 
            Component.literal("🏆 SEEKERS VENCERAM! 🏆").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) :
            Component.literal("🏆 HIDERS VENCERAM! 🏆").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        
        CustomBossEvent resultBar = new CustomBossEvent(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hideseek", "result"),
            message
        );
        resultBar.setColor(BossEvent.BossBarColor.PURPLE);
        resultBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
        
        // Adicionar todos os jogadores
        GameManager gameManager = GameManager.getInstance();
        gameManager.getPlayerManager().getHiders().forEach(resultBar::addPlayer);
        gameManager.getPlayerManager().getSeekers().forEach(resultBar::addPlayer);
        gameManager.getPlayerManager().getSpectators().forEach(resultBar::addPlayer);
        
        resultBar.setProgress(1.0f);
        
        // Remover após 5 segundos
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                resultBar.removeAllPlayers();
            }
        }, 5000);
    }
    
    // Variáveis para controle de avisos de tempo
    private boolean warningAt60Sent = false;
    private boolean warningAt30Sent = false;
    private boolean warningAt10Sent = false;
    private boolean warningAt5Sent = false;
    
    /**
     * Envia avisos de tempo em momentos críticos
     */
    private void sendTimeWarnings(GameState phase, int remainingSeconds) {
        ChatManager chatManager = ChatManager.getInstance();
        MinecraftServer server = GameManager.getInstance().getServer();
        
        if (server == null) return;
        
        String phaseName = switch (phase) {
            case HIDING -> "ESCONDER";
            case SEEKING -> "BUSCAR";
            default -> "";
        };
        
        // Avisos apenas para fases HIDING e SEEKING
        if (phase != GameState.HIDING && phase != GameState.SEEKING) return;
        
        // Aviso de 60 segundos
        if (remainingSeconds == 60 && !warningAt60Sent) {
            chatManager.timeWarning(server, remainingSeconds, phaseName);
            warningAt60Sent = true;
        }
        // Aviso de 30 segundos
        else if (remainingSeconds == 30 && !warningAt30Sent) {
            chatManager.timeWarning(server, remainingSeconds, phaseName);
            warningAt30Sent = true;
        }
        // Aviso de 10 segundos
        else if (remainingSeconds == 10 && !warningAt10Sent) {
            chatManager.timeWarning(server, remainingSeconds, phaseName);
            warningAt10Sent = true;
        }
        // Aviso de 5 segundos
        else if (remainingSeconds == 5 && !warningAt5Sent) {
            chatManager.timeWarning(server, remainingSeconds, phaseName);
            warningAt5Sent = true;
        }
    }
    
    /**
     * Reseta os avisos de tempo para uma nova fase
     */
    private void resetTimeWarnings() {
        warningAt60Sent = false;
        warningAt30Sent = false;
        warningAt10Sent = false;
        warningAt5Sent = false;
    }
}