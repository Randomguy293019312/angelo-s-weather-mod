# Meteorologia — mod de clima para Minecraft

Tornados e nuvens renderizados com **ray marching**, supercélulas, chuva forte, radar,
estação meteorológica, balão de ar quente e itens de sobrevivência.

| | |
|---|---|
| Minecraft | **1.21.1** |
| Mod loader | **NeoForge 21.1.92** (ou mais novo da série 21.1.x) |
| Java | 21 |

> ## ⚠️ Leia isto primeiro: o `.jar` ainda precisa ser compilado
> O ambiente onde este mod foi escrito não tem internet, então **não foi possível baixar
> o Minecraft/NeoForge e compilar**. O que você recebeu é o **projeto completo** — o `.jar`
> sai com um comando (ou sem instalar nada, pelo GitHub). Veja abaixo.
> O código **não foi compilado nem testado dentro do jogo**; veja a seção *Status*.

---

## 1. Como gerar o `.jar`

### Opção A — sem instalar nada (GitHub Actions) ✅ mais fácil
1. Crie um repositório vazio no GitHub e envie **todo o conteúdo desta pasta** (inclusive `.github/`).
2. Abra a aba **Actions** → workflow **"Build do mod"** (roda sozinho a cada push; ou clique em *Run workflow*).
3. Quando terminar, clique na execução → **Artifacts → `meteorologia-jar`** → baixe o zip. Dentro está o `.jar`.

### Opção B — IntelliJ IDEA (Community, grátis)
1. *File → Open* e escolha esta pasta (ele reconhece o Gradle e baixa tudo sozinho; a 1ª vez demora).
2. Painel **Gradle → Tasks → build → build**.
3. O arquivo fica em `build/libs/meteorologia-1.0.0.jar`.

### Opção C — linha de comando
Precisa de **JDK 21** e **Gradle 8.12+** instalados.
```bash
gradle build
```
Resultado: `build/libs/meteorologia-1.0.0.jar`.
(Opcional: rode `gradle wrapper` uma vez para gerar o `gradlew` e depois use `./gradlew build`.)

## 2. Instalar e jogar
1. Instale o **NeoForge 1.21.1** (https://neoforged.net) e abra o jogo uma vez.
2. Copie o `.jar` para `.minecraft/mods/`.
3. Entre num mundo (com cheats) e teste: `/meteo tornado criar 3`

---

## 3. O que tem no mod

### Tempestades
* **Tornado** (escala EF0–EF5) desenhado por **ray marching** num shader próprio: funil de condensação
  com giro e ondulação, poeira/detritos na base e **nuvem-parede** rotativa. Ele nasce descendo da nuvem,
  amadurece e se desfaz em "corda" fina. O terreno oculta o tornado corretamente (buffer de profundidade).
* **Supercélula**: tempestade gigante (base escura achatada, torre e bigorna) visível **à distância**, que
  se aproxima do jogador. As nuvens indicam a tempestade e **acompanham** o tornado.
* **Chuva forte**: quando uma supercélula chega perto, a chuva fica **local** (só onde você está), com neblina
  densa e escurecimento. Chuva forte/trovoada aumentam a chance de nascer supercélula e tornado.
* **Efeitos do tornado**: puxa, gira e arremessa entidades; causa dano (EF1+); arranca blocos conforme a
  resistência (folhas/vidro no EF0–1 … pedra no EF4–5) e lança **detritos** (blocos voadores).
  **Estar 4+ blocos abaixo da superfície protege** (porão/abrigo). Agachar reduz a força do vento.
* **Raios** perto de jogadores durante a supercélula.

### Blocos
| Bloco | Função |
|---|---|
| **Radar Meteorológico** | Clique direito abre a tela do radar: ecos de supercélulas (verde→vermelho→magenta), tornados com "EFn", varredura, alcance 1–8 km. |
| **Anemômetro** | Mostra o vento; emite redstone proporcional (0–15). |
| **Barômetro** | Pressão em hPa; sinal sobe quando a pressão cai (tempestade chegando). |
| **Termo-higrômetro** | Temperatura e umidade; sinal pela temperatura. |
| **Estação Meteorológica** | Tudo junto + lista de ameaças; sinal 15 = tornado próximo. |
| **Pluviômetro** | Acumula chuva (mm); comparador lê o nível; agachado + clique esvazia. |
| **Sirene de Tempestade** | Toca sozinha com tornado a menos de 1,5 km, ou com redstone. |

### Itens de sobrevivência
* **Rádio Meteorológico** — clique direito = previsão (distância, direção, velocidade, tempo estimado).
  Na hotbar, **avisa sozinho** de tornado/tempestade próxima.
* **Medidor Portátil** — vento, pressão, temperatura e umidade onde você está.
* **Kit de Emergência** — Absorção, Resistência, Regeneração e Queda Lenta por 2 min.
* **Barra Energética** — comida rápida. **Botijão de Propano** — combustível do balão.

### Balão de ar quente
Item *Balão de Ar Quente*: clique num bloco para colocar; clique direito no balão para entrar (2 lugares).
* **W** acende o queimador (sobe) · **S** ventila (desce) · **Shift** desce do balão.
* Clique direito com **Botijão de Propano** reabastece (máx. 3 min de queimador).
* O balão **vai com o vento da altitude**, e o vento **gira com a altura**: você "dirige" subindo e descendo.
  Tornados e tempestades puxam o balão — cuidado.

### Comandos (precisa ser OP nível 2) — `/meteo` ou `/meteorologia`
```
/meteo tornado criar [ef 0-5]        cria um tornado ~80 blocos à frente (com supercélula-mãe)
/meteo tornado limpar
/meteo supercelula criar [distancia] supercélula que se aproxima (padrão 1500 blocos)
/meteo supercelula limpar
/meteo chuva leve|moderada|forte|limpar
/meteo tempestade iniciar|parar      chuva forte + supercélula vindo na sua direção
/meteo vento                         vento na sua posição
/meteo info                          tempestades ativas (distância, direção, ETA)
```

### Configuração (`config/meteorologia-common.toml` e `-client.toml`)
`tempestadesNaturais`, `frequencia`, `maxSupercelulas`, `tornadoDestroiBlocos`, `maxDetritos`, `raios`
e, no cliente, `rayMarching` (liga/desliga), `passos` (qualidade: 24 rápido / 48 normal / 96 bonito) e `neblinaTempestade`.

---

## 4. Status: o que foi validado e o que NÃO foi

**Validado de verdade (rodando):**
* ✅ **O shader de ray marching** foi compilado e **executado** num contexto OpenGL real (Mesa/llvmpipe,
  GLSL estrito) usando os mesmos uniformes que o Java envia, incluindo oclusão por terreno.
  Imagens em `docs/` e scripts em `tools/`. Isso achou e corrigiu um erro de lógica (funil na fase de corda).
* ✅ Sintaxe de todos os 31 arquivos Java (parser do `javac`), todos os JSON, e referências cruzadas entre classes.
* ✅ Texturas geradas e conferidas visualmente (`docs/preview_texturas.png`).

**NÃO validado:**
* ❌ **Compilação contra o Minecraft/NeoForge** (sem internet no ambiente). Pode haver pequenos erros de
  nome de API. Se o build falhar, **me mande o log de erro** que eu corrijo.
* ❌ **Teste dentro do jogo** (comportamento real, desempenho, balanceamento do dano/vento).
* ❌ Shader em GPUs reais (NVIDIA/AMD/Intel), com Sodium/Iris ou modo gráfico "Fabulous".

## 5. Limitações e dicas
* **Desempenho:** ray marching é pesado. Se cair o FPS, reduza `passos` no config do cliente.
* **Shaders do Iris/Optifine:** podem conflitar; desligue-os ou use `rayMarching=false`.
  Sem o shader (ou se ele falhar ao compilar) o mod usa um funil de **partículas** como plano B.
* O clima vanilla é **global**: quando a supercélula está em cima de alguém, o servidor liga chuva/trovoada
  para todos. A chuva "local" e a neblina são só visuais, por jogador.
* As tempestades **não persistem** ao fechar o mundo.
* Mensagens do jogo estão em português; nomes de itens/blocos em português e inglês.
* Não há sons próprios (usa sons do jogo). Texturas são geradas por código (`tools/gen_assets.py`) e podem ser trocadas.

## 6. Estrutura
```
src/main/java/com/meteorologia/
  weather/   StormManager (servidor), WindModel, Supercell, Tornado
  client/    StormRenderer (ray marching), ClientEvents, RadarScreen, balão (modelo/renderer)
  block/ item/ entity/ command/ net/
src/main/resources/assets/meteorologia/shaders/core/storm.fsh   <- o shader de ray marching
```
