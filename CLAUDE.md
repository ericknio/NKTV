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

## Cofre Obsidian (`APK/`)
Só conteúdo do projeto Android. Nada do projeto de PC.
- `Versões/` → uma nota por release: o que mudou.
- `Decisões/` → só escolhas que o código não mostra, com o porquê.
- `Bugs/Abertos.md` → bugs abertos e resolvidos (sintoma, causa, versão).
- **Atualize ao concluir:** nova versão, bug achado ou resolvido, decisão relevante.
- Notas curtas, com links `[[...]]`. Sem histórico de conversa nem o que o código já mostra.
- Leia só a nota que a tarefa pede, não o cofre inteiro.

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

## Status Atual — v1.0.9 (produção)

### Em produção (v1.0.9)
- Publicado no GitHub (`ericknio/NKTV`) e disponível em `app.fbtec.site`.
- Inclui: paginação do catálogo de filmes (páginas de 300, Home com 5.000), limite de 3 reconexões automáticas no player, barras do guia ao vivo avançando, posição do filme salva ao sair, densidade da interface fixa por celular (TV inalterada).
- Release: `assembleRelease` exige `KEYSTORE_PASSWORD` e `KEY_PASSWORD` no ambiente. Publicar com `gh release create <versão> NKTV.V<versão>.apk` (tag sem `v`, um único APK versionado).

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
