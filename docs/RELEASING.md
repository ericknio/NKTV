# Release do NKTV (Android)

**Quem faz o quê:** o Antigravity compila e publica. O Maestro-Claude confere o APK **antes** e
**depois** de publicar. Commit, push e release só acontecem com a validação do usuário.

Publicar dispara o auto-update nos aparelhos dos usuários. Um APK errado não tem como ser
desfeito, só substituído por uma versão nova. Por isso a conferência vem antes da publicação.

## 1. Preparar e compilar (Antigravity)

1. Combinar a versão nova com o usuário. Tem que ser maior que a publicada.
2. `app/build.gradle.kts`: `versionCode` +1 e `versionName` novo.
3. `app/src/main/java/app/opentv/ui/settings/AboutScreen.kt`, em `PatchNotesData`: nova entrada
   no topo com `isLatest = true`; a anterior perde o `isLatest`. Texto curto, sem detalhe técnico.
4. Criar `release_notes_<versão>.txt` na raiz (não vai para o git): 2 ou 3 linhas curtas.
5. Compilar com as senhas no ambiente (sem elas o `assembleRelease` falha de propósito):
   ```powershell
   $env:KEYSTORE_PASSWORD = '<senha>'; $env:KEY_PASSWORD = '<senha>'
   .\gradlew.bat testDebugUnitTest assembleRelease
   ```
   Keystore: `~/.android/nktv-release.jks` (ou `KEYSTORE_PATH`). Não criar keystore novo.
6. Copiar `app/build/outputs/apk/release/app-release.apk` para `NKTV.V<versão>.apk` na raiz.
7. **PARAR.** Avisar o Maestro-Claude (`maestri ask`) com o caminho e o SHA-256 do APK.
   Não commitar nem publicar antes do OK.

## 2. Conferir o APK (Maestro-Claude), antes de publicar

- `aapt2 dump badging`: pacote `com.nktv.app`, `versionCode` e `versionName` certos.
- `apksigner verify --print-certs`: o certificado SHA-256 tem que ser **igual** ao do APK
  anterior (`65cf342a…84bd`). Se for diferente, **não publicar**: o update quebraria.
- Com um aparelho conectado: instalar a versão anterior oficial e atualizar por cima
  (`adb install -r`). O app tem que abrir sem travar.
- Banco Room: se `app/schemas` mudou, a versão nova precisa de `Migration` (não existe mais
  `fallbackToDestructiveMigration`).

## 3. Publicar (Antigravity, depois do OK)

1. Última linha do `release_notes_<versão>.txt`, **dentro de um comentário HTML**, neste formato exato:
   `<!-- SHA-256 (NKTV.V<versão>.apk): <hash> -->`
   O comentário não aparece no GitHub nem no aviso de atualização do app (o `cleanNotes` corta a
   partir de `<!--`). O patch note deve ter só as novidades, sem SHA nem texto técnico.
2. Commit só dos arquivos rastreados (`git add -u`), sem o cofre `APK/` nem `.maestri/`:
   `feat: release <versão> - <resumo curto>`, depois `git push origin main`.
3. Criar a release:
   ```powershell
   gh release create <versão> NKTV.V<versão>.apk --title "NKTV <versão>" --notes-file release_notes_<versão>.txt --target main --latest
   ```
   - Tag **sem** o `v` (ex.: `1.0.9`). Usar `--target main`: o SHA abreviado dá erro 422.
   - **Um único `.apk` por release.** O painel `app.fbtec.site` pega o primeiro que achar.

## 4. Conferir depois (Maestro-Claude)

- `releases/latest` aponta para a tag nova.
- O APK baixado do GitHub tem o mesmo SHA-256 do arquivo conferido.

## Regras que não mudam

- **Nunca gerar uma chave nova.** O `ApkInstaller` e o Android exigem o mesmo certificado; uma
  chave nova impede todos de atualizarem. Para trocar a senha, mude só a senha
  (`keytool -storepasswd` e `-keypasswd`) e atualize o secret `KEYSTORE_PASSWORD` do GitHub.
- Perder o keystore é irrecuperável. Mantenha uma cópia fora deste computador.
- O workflow `.github/workflows/release.yml` só dispara em tags `v*`. Como a tag é sem `v`, ele
  não roda no fluxo normal.
