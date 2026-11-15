# NKTV - Streaming Multiplataforma Brasil

Aplicativo moderno, fluido e de alta performance para reprodução de IPTV e Streaming, desenvolvido com foco em Smart TVs, TV Boxes e dispositivos Android.

---

## 📺 Principais Recursos

- **⚡ Performance e Fluidez:** Código otimizado para rodar com rapidez mesmo em aparelhos mais limitados (como Xiaomi Mi TV Stick, TVs TCL, Philco e Fire TV).
- **📡 TV Ao Vivo (Canais):**
  - **Agrupamento Inteligente de Qualidades:** Canais duplicados (FHD, HD, SD, 4K) são unificados automaticamente em um único canal com seletor de resolução.
  - **Qualidade Adaptativa:** Inicia sempre na melhor qualidade e desce suavemente caso a conexão oscile, evitando travamentos.
  - **Buffer Adaptativo:** Sistema de contingência que amplia a reserva de dados em caso de instabilidade na rede.
  - **Watchdog Anti-Congelamento:** Recupera automaticamente o sincronismo de vídeo sem travar a imagem enquanto o áudio toca.
  - **Mini-Player no Guia:** Pré-visualização ao vivo ampliada para navegação rápida e intuitiva.
- **🎬 Filmes e Séries (VOD):**
  - Barra de pesquisa integrada e em tempo real diretamente nas abas de Filmes e Séries.
  - Histórico de reprodução e recurso de continuar de onde parou.
  - Catálogo organizado por categorias e gêneros.
- **🎯 100% Otimizado para Controle Remoto (D-Pad First):** Navegação pensada para uso ágil na TV.
- **🇧🇷 Interface em Português do Brasil:** Telas, diálogos e mensagens totalmente traduzidos.
- **🖼️ Suporte a Banners de TV:** Ícone retangular padrão Android TV (16:9) e ícone circular em telas convencionais.

---

## 📱 Dispositivos Compatíveis

- **Android TV / Google TV** (Xiaomi, TCL, Philco, Sony, Philips, etc.)
- **Amazon Fire TV Stick**
- **TV Boxes Android**
- **Smartphones e Tablets Android**

---

## 🛠️ Como Compilar

### Pré-requisitos
- JDK 17+
- Android SDK (API 35)

```bash
# Clonar o repositório
git clone https://github.com/SEU-USUARIO/NKTV.git
cd NKTV

# Compilar versão de depuração (Debug)
./gradlew assembleDebug

# Compilar versão final (Release)
./gradlew assembleRelease
```

O arquivo `.apk` gerado estará disponível em `app/build/outputs/apk/release/`.

---

## ⚖️ Licença e Aviso Legal

Distribuído sob a licença [GPL-3.0](LICENSE).  
O NKTV é um reprodutor de mídia e não hospeda nem fornece qualquer conteúdo protegido por direitos autorais. O usuário é responsável pelas credenciais e listas configuradas.
