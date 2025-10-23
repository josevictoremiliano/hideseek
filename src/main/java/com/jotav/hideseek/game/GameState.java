package com.jotav.hideseek.game;

/**
 * Estados possíveis do jogo Hide and Seek
 */
public enum GameState {
    /**
     * Aguardando jogadores e configuração
     */
    LOBBY,
    
    /**
     * Contagem regressiva para início (10 segundos)
     */
    STARTING,
    
    /**
     * Fase de esconder - Hiders se escondem, Seekers imobilizados
     */
    HIDING,
    
    /**
     * Fase de busca - Seekers procuram os Hiders
     */
    SEEKING,
    
    /**
     * Jogo terminou, anúncio de vencedor
     */
    ENDING
}