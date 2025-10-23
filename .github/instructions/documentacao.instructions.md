---
applyTo: '**'
---
# Roteiro do Mod Minecraft: Hide and Seek

Este documento detalha os comandos, estados e ações necessárias para o desenvolvimento de um mod de Esconde-Esconde (Hide and Seek) para Minecraft.

---

## 1. Comandos Essenciais

| Comando | Descrição | Detalhes e Ações Adicionais | Permissão |
| :--- | :--- | :--- | :--- |
| **`/hns join`** | Entrar na fila de espera (lobby) do jogo. | Adiciona o jogador à fila. Se o jogo estiver em andamento, o jogador pode ser colocado como espectador (opcional) ou impedido de entrar. | Todos |
| **`/hns leave`** | Sair do jogo ou da fila de espera. | Remove o jogador e o teleporta para o local de *Início do Game* (`/hns set lobby`). | Todos |
| **`/hns stop`** | Comando para forçar a parada total do jogo. | Encerra todos os timers, remove efeitos, limpa inventários de itens específicos do jogo e teleporta *todos* os jogadores para o local de *Início do Game* (`/hns set lobby`). | OP |
| **`/hns lobbykick`** | Remover *todos* os jogadores do jogo (reset rápido do servidor). | Ação idêntica ao `/hns stop`, mas com foco em 'reset' de estado. | OP |
| **`/hns randomize <min_hiders> <max_hiders>`** | Randomizar a seleção dos jogadores para os times. | Distribui os jogadores na fila (lobby) para os times, garantindo que o número de Hiders esteja entre `min_hiders` e `max_hiders`. | OP |
| **`/hns set teams <hiders_count> <seekers_count>`** | Definir o número *exato* de vagas para Hiders e Seekers. | Define o limite de jogadores para cada time (se o número total de jogadores for suficiente). | OP |
| **`/hns set lobby`** | Define o local de *respawn pós-jogo* e o local do *lobby/espera*. | Ponto de teleport final e de espera. | OP |
| **`/hns set seekerspawn`** | Define o ponto de *spawn inicial dos Seekers* (local de aprisionamento). | **Ação:** Aplica os efeitos `slowness 255` e `jump_boost 255` (ou similar) para imobilizar totalmente os Seekers no início. | OP |
| **`/hns set mapboundary <pos1> <pos2>`** | **NOVO:** Define os limites do mapa jogável. | Cria uma *região* de jogo. Jogadores que saírem desta área podem ser automaticamente teleportados de volta ou eliminados. | OP |
| **`/hns set time <phase> <seconds>`** | Configurar a duração das fases do jogo. | **Fases:** `HIDE` (esconder), `SEEK` (procurar), `GRACE` (opcional, tempo extra de fuga). Ex: `/hns set time SEEK 300`. | OP |
| **`/hns start`** | Forçar o início do jogo. | Inicia a contagem regressiva e transiciona o jogo do estado *Lobby* para *Esconder*. | OP |
| **`/hns set team <player> <team>`** | Forçar um jogador a um time específico (Hider ou Seeker). | Uso: `/hns set team Notch Hider`. | OP |
| **`/hns checkconfig`** | **NOVO:** Verificar todas as configurações salvas. | Lista todos os pontos (`lobby`, `seekerspawn`, etc.) e tempos, informando se algo essencial está faltando. | OP |

---

## 2. Estados do Jogo e Transições

O mod deve progredir através dos seguintes estados:

| Estado | Descrição | Ações Principais |
| :--- | :--- | :--- |
| **LOBBY** | Espera por jogadores e configuração. | O placar mostra contagem de jogadores e requisitos para início. Jogadores entram com `/hns join`. |
| **STARTING** | Contagem regressiva para início da rodada. | Ativado ao atingir o mínimo de jogadores ou por `/hns start`. Exibe contagem regressiva de 10 segundos via `Title` ou `BossBar`. |
| **HIDING** | Tempo para os Hiders se esconderem. | **Seekers:** Imobilizados e cegos no *Seeker Spawn*. **Hiders:** Liberados no mapa. O timer `HIDE` é ativado. |
| **SEEKING** | Tempo para os Seekers procurarem. | **Seekers:** Efeitos de imobilização/cegueira removidos. Liberados para o mapa. O timer `SEEK` é ativado. |
| **ENDING** | O jogo terminou. | Ativado por: (1) Timer `SEEK` acabou, ou (2) Todos os Hiders foram encontrados. Anúncio do time vencedor. Transição automática para o estado **LOBBY** após 10 segundos. |

---

## 3. Explicações e Ações Detalhadas

### 3.1. Feedback Visual e Notificações

* **Barra de Chefe (`BossBar`):** Usada para exibir de forma proeminente o **Timer de Jogo** (Tempo restante) e o **Número de Hiders Restantes**.
* **Mensagem Centralizada/Título (`Title`):** Usada para **Anúncios Importantes** (Ex: "30 Segundos Restantes!", "Seekers Liberados!", "Hiders Vencem!").
* **Mensagem no Chat:** Usada para notificar **Ações de Jogador** (Ex: "Fulano de Tal foi pego por Sicrano! [3 Hiders restantes]").

### 3.2. Configuração Inicial e Verificação

* **Persistência:** Todos os comandos de `set` (`/hns set lobby`, `/hns set seekerspawn`, etc.) devem salvar as coordenadas de forma persistente (ex: em um arquivo de configuração do mod).
* **`checkconfig`:** O comando `/hns checkconfig` deve ser usado pelo OP antes de iniciar a primeira partida para confirmar que todos os pontos de spawn e tempos foram configurados.

### 3.3. Início do Jogo (`STARTING` -> `HIDING`)

1.  **Limpeza:** Remover todos os efeitos de poção (exceto os de imobilização nos Seekers, se aplicável) e limpar o inventário de todos os participantes.
2.  **Atribuição:** Distribuir jogadores aleatoriamente ou conforme configuração para os times.
3.  **Equipamento:** Distribuir itens essenciais (ex: Hiders podem receber um item cosmético, Seekers podem receber uma bússola que aponta para o Hider mais próximo - opcional).
4.  **Teleporte:** Hiders para o mapa (em pontos aleatórios ou pré-definidos), Seekers para o *Seeker Spawn*.
5.  **Placar:** Inicializar o Scoreboard para mostrar os times e a contagem de Hiders.

### 3.4. Mecanismo de Descoberta (Durante `SEEKING`)

* **Como Pegar:** Um Seeker deve dar um **hit/ataque** em um Hider para capturá-lo.
* **Resultado:** Ao ser pego, o Hider deve ser:
    1.  **Eliminado:** O Hider se torna um **Espectador** (modo padrão). Ele não pode interagir com o jogo, mas pode ver o restante da partida.
    2.  **(Modo Opcional):** O Hider se transforma em um **Seeker** (modo "Infection").

### 3.5. Condições de Vitória e Fim do Jogo

* **Vitória dos Seekers:**
    * Todos os Hiders foram descobertos e eliminados.
* **Vitória dos Hiders:**
    * O timer de `SEEK` acaba e **pelo menos um** Hider permanece escondido.
* **Ação Final:** O mod deve anunciar o time vencedor com clareza, dar um pequeno tempo de celebração e, em seguida, teleportar todos de volta ao **LOBBY** (`/hns set lobby`).

---