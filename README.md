
# Hide and Seek - Minecraft Mod

Um mod multiplayer de Esconde-Esconde para Minecraft 1.21.1 usando NeoForge 21.1.213.

## 🎮 Funcionalidades Implementadas

### ✅ Sistema de Comandos
- `/hns join` - Entrar na fila do jogo
- `/hns leave` - Sair do jogo
- `/hns start` - Iniciar jogo (OP)
- `/hns stop` - Parar jogo (OP)
- `/hns set lobby` - Definir spawn do lobby (OP)
- `/hns set seekerspawn` - Definir spawn dos Seekers (OP)
- `/hns set mapboundary <pos1> <pos2>` - Definir limites do mapa (OP)
- `/hns checkconfig` - Verificar configurações (OP)
- `/hns scoreboard show` - Mostrar scoreboard (OP)
- `/hns scoreboard hide` - Ocultar scoreboard (OP)
- `/hns leaveall` - Remove todos os jogadores do jogo (OP)
- `/hns stats [player]` - Ver estatísticas (próprias ou de outro jogador)
- `/hns leaderboard [category]` - Ver ranking dos melhores jogadores
- `/hns globalstats` - Ver estatísticas globais do servidor

### ✅ Estados do Jogo
O jogo segue o fluxo: **LOBBY → STARTING → HIDING → SEEKING → ENDING → LOBBY**

- **LOBBY**: Aguardando jogadores
- **STARTING**: Contagem regressiva (10s padrão)
- **HIDING**: Hiders se escondem, Seekers imobilizados (60s padrão)
- **SEEKING**: Seekers procuram Hiders (300s padrão)
- **ENDING**: Anúncio do vencedor e retorno ao lobby

### ✅ Sistema de Times
- **Hiders** (Verde): Se escondem e tentam sobreviver
- **Seekers** (Vermelho): Procuram e capturam Hiders
- **Espectadores** (Cinza): Hiders capturados

### ✅ Interface do Usuário
- **BossBar**: Mostra contagem regressiva e tempo restante
- **Scoreboard**: Exibe estado do jogo e contagem de times (só durante partidas)
- **Teams**: Cores diferentes para identificar jogadores
- **Controle de UI**: Scoreboard oculto por padrão, aparece apenas durante jogos

### ✅ Sistema de Efeitos
- **Seekers durante HIDING**: Slowness 255 + Cegueira + Jump Boost negativo
- **Teleportes seguros** com verificação de posições válidas
- **Verificação de limites** do mapa

### ✅ Configuração Persistente
Todas as configurações são salvas no arquivo `hideseek-common.toml`:
- Tempos das fases
- Pontos de spawn
- Limites do mapa
- Número de jogadores por time

## 🚀 Como Usar

### Configuração Inicial
1. Defina o lobby: `/hns set lobby`
2. Defina spawn dos Seekers: `/hns set seekerspawn`
3. (Opcional) Defina limites do mapa: `/hns set mapboundary <pos1> <pos2>`
4. Verifique configurações: `/hns checkconfig`

### Jogando
1. Jogadores entram com `/hns join`
2. Admin inicia com `/hns start` (mín. 2 jogadores)
3. Hiders se escondem durante fase HIDING
4. Seekers são liberados na fase SEEKING
5. Seekers capturam Hiders atacando eles
6. Vitória: Seekers capturam todos OU tempo esgota (Hiders vencem)

### Visualizando Estatísticas
- **Suas stats**: `/hns stats`
- **Stats de outro jogador**: `/hns stats NomeDoJogador`
- **Ranking geral**: `/hns leaderboard`
- **Rankings específicos**: 
  - `/hns leaderboard wins` - Mais vitórias
  - `/hns leaderboard winrate` - Melhor taxa de vitória
  - `/hns leaderboard hider` - Melhores Hiders
  - `/hns leaderboard seeker` - Melhores Seekers  
  - `/hns leaderboard captures` - Mais capturas feitas
  - `/hns leaderboard survival` - Maior tempo de sobrevivência
  - `/hns leaderboard streak` - Maior sequência de vitórias
- **Estatísticas do servidor**: `/hns globalstats`

## 🛠️ Desenvolvimento

### Estrutura do Código
- `game/` - Lógica principal (GameManager, PlayerManager, GameState)
- `commands/` - Sistema de comandos (`/hns`)
- `ui/` - BossBar e Scoreboard
- `effects/` - Efeitos nos jogadores e teleportes
- `events/` - Event handlers (captura, limites do mapa)
- `util/` - Utilitários (conversão de coordenadas)
- `stats/` - Sistema de estatísticas (PlayerStats, StatsManager)
- `chat/` - Gerenciamento de mensagens de chat

### Compilação
```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew compileJava

# Executar cliente de teste
.\gradlew runClient

# Executar servidor de teste
.\gradlew runServer
```

### ✅ Sistema de Chat
- **Mensagens de entrada/saída**: Notificações quando jogadores entram ou saem do jogo
- **Avisos de estado**: Contagem regressiva, formação de times, início das fases
- **Eventos de captura**: Anúncios quando Hiders são capturados
- **Avisos de tempo**: Alertas em 60s, 30s, 10s e 5s antes do fim de cada fase
- **Mensagens de vitória**: Parabeniza vencedores com mensagens personalizadas
- **Cores e formatação**: Usa ChatFormatting para destacar informações importantes

### ✅ Sistema de Estatísticas e Rankings
- **Coleta automática**: Registra todas as partidas, vitórias, derrotas, capturas e tempos
- **Estatísticas individuais**: `/hns stats` mostra detalhes completos de cada jogador
- **Rankings múltiplos**: Leaderboards por vitórias, taxa de vitória, capturas, sobrevivência, etc.
- **Persistência**: Dados salvos em `hideseek_stats.json` que persiste entre reinicializações
- **Métricas avançadas**: Streaks de vitórias, tempo total jogado, recordes pessoais
- **Estatísticas globais**: Visão geral do servidor com `/hns globalstats`

### ✅ Sistema de Espectador
- **Modo Spectator**: Hiders capturados entram automaticamente em modo espectador
- **Capacidades**: Podem voar, atravessar blocos e observar o jogo sem interferir
- **Invisibilidade**: Efeito aplicado para garantir que não sejam vistos pelos jogadores ativos
- **Restauração**: Gamemode original restaurado automaticamente ao final do jogo

### Próximos Passos
- [ ] Sistema de spawn aleatório para Hiders
- [ ] Integração com outros mods (JEI, etc.)

## 📋 Requisitos
- Minecraft 1.21.1
- NeoForge 21.1.209
- Java 21+

## 📄 Licença
All Rights Reserved

---

Para mais informações sobre NeoForge:
- [Documentação](https://docs.neoforged.net/)
- [Discord](https://discord.neoforged.net/)
