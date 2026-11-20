# NKTV Android APK — Contexto para Claude Code

> Histórico completo de decisões e conversas: [CHAT_ANTIGRAVITY.md](./CHAT_ANTIGRAVITY.md)

---

## Regras do Projeto (Ponytail)
- Mínimo de código novo. Sem bibliotecas desnecessárias.
- Não faça commit, push ou release final sem validação do usuário.
- **Validação obrigatória** após toda mudança Kotlin:
  ```powershell
  .\gradlew.bat compileDebugKotlin
  ```
- Release: `.\gradlew.bat assembleRelease` → arquivo gerado: `NKTV.V<versão>.apk`.

---

## Arquitetura Resumida
- **Kotlin + Jetpack Compose** (Material3), `minSdk 23`, `targetSdk 35`
- Player: **ExoPlayer / Media3** via `PlayerController.kt`
- BD local: **Room** (`OpenTvDatabase`, DAOs em `Daos.kt`)
- Imagens: **Coil** (limitado a 4 threads paralelas, RGB_565, cache de 256MB)
- Fonte de dados: Xtream Codes / M3U / Stalker / Stremio
- Pastas-chave:
  - `ui/channels/` → tela ao vivo, guide, player de canais
  - `ui/vod/` → filmes, séries, player VOD
  - `ui/settings/` → configurações, sobre, EPG
  - `data/` → modelos Room, DAOs, parsers, repositórios
  - `player/` → PlayerController, fila de reprodução

---

## Status Atual — v1.0.5 (produção) / v1.0.6 (em desenvolvimento local)

### Em produção (v1.0.5)
- Publicado no GitHub (`ericknio/NKTV`) e disponível em `app.fbtec.site`.

### Implementado localmente para v1.0.6 (aguardando validação do usuário)
1. Modo imersivo (tela cheia no celular) — `MainActivity.kt`
2. Patch Notes em Configurações > Sobre — `AboutScreen.kt`
3. Reset de scroll ao trocar categoria — `HomeScreen.kt`, `GuideGrid.kt`, `VodScreens.kt`
4. Busca sem teclado automático (Android TV) — `VodScreens.kt`
5. Otimizações de memória/CPU (largeHeap, Coil limitado, collapseVariants em background)
6. Sincronização "Continuar Assistindo" ↔ Filmes/Séries — `VodScreens.kt`, `VodDetailScreens.kt`
7. ±10s com seta do controle no player — `VodPlayerScreen.kt`, `PlayerController.kt`
8. Botão play/continuar na tela de série — `VodDetailScreens.kt`
9. Auditoria de performance: `remember { System.currentTimeMillis() }` em `GuideGrid`, `ChannelList`, `ChannelRow`; cache de labels em `TimeHeader`

---

## Economia de Tokens — Roteamento de Complexidade

Antes de executar qualquer tarefa, delegue para o subagente adequado:

| Complexidade | Agente | Exemplos |
|---|---|---|
| Simples | `@agente-economico` | Buscar arquivo, grep, git status, explicar função |
| Média | `@agente-padrao` | Criar componente, corrigir bug em 1 arquivo, escrever teste |
| Alta | Opus (sessão principal) | Arquitetura, bugs cross-módulo, race conditions |

**Regras:**
- Nunca leia arquivo inteiro quando um `grep` resolve.
- Delegue buscas exploratórias ao `@agente-economico`.
- Respostas diretas, sem repetir código não alterado.
- O `.gitignore` já exclui `build/`, `.gradle/`, `graphify-out/` — não os leia.
