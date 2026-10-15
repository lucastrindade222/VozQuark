# VozQuark

Transforme texto em voz, direto no seu computador.

O **VozQuark** é um projeto de estudo feito com **Java** e **Quarkus** que converte texto em áudio (WAV) usando o [Piper TTS](https://github.com/rhasspy/piper), com voz em português do Brasil. Tudo roda localmente, sem enviar nada para a internet.

<!-- Adicione aqui uma captura de tela da interface:
![Tela do VozQuark](docs/screenshot.png)
-->

## Funcionalidades

- Interface web moderna (HTML, SCSS e JS) servida pelo próprio Quarkus
- Geração de áudio com a voz `pt_BR-faber-medium`
- Player integrado e download do arquivo `.wav`
- Validação do texto (de 3 a 500 caracteres)
- Proteção contra sobrecarga: limite de execuções simultâneas e de requisições por minuto (respostas `429`)
- Abre o navegador automaticamente ao iniciar
- Testes automatizados com JUnit, RestAssured e Mockito

## Download

Baixe a versão pronta para o seu sistema na página de **[Releases](https://github.com/lucastrindade222/VozQuark/releases)**. Os pacotes já incluem o Java, o Piper e o modelo de voz, então não é preciso instalar mais nada.

| Sistema | Arquivo | Como executar |
|---|---|---|
| Windows | `VozQuark-windows.zip` | Extraia e abra `VozQuark\VozQuark.exe` |
| Linux | `VozQuark-linux.tar.gz` | `tar -xzf VozQuark-linux.tar.gz` e depois `./VozQuark/bin/VozQuark` |

O navegador abre sozinho em `http://localhost:8080`.

> **Aviso do Windows:** como o executável não é assinado, o SmartScreen pode mostrar "editor desconhecido". Clique em **Mais informações** e depois em **Executar assim mesmo**.

## Como rodar a partir do código-fonte

### Requisitos

- JDK 21 (ou a versão definida no `pom.xml`)
- Piper e o modelo de voz (veja abaixo)

### 1. Baixar o Piper e o modelo

Na raiz do projeto (exemplo para Linux x86_64):

```bash
mkdir -p opt && cd opt
wget https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_linux_x86_64.tar.gz
tar -xzf piper_linux_x86_64.tar.gz

cd piper
BASE=https://huggingface.co/rhasspy/piper-voices/resolve/main/pt/pt_BR/faber/medium
wget $BASE/pt_BR-faber-medium.onnx
wget $BASE/pt_BR-faber-medium.onnx.json
```

No Windows, use o arquivo `piper_windows_amd64.zip` da mesma página de releases do Piper.

### 2. Configuração

O arquivo `src/main/resources/application.properties` define os caminhos e os limites:

```properties
quarkus.http.port=8080

tts.piper.bin=${TTS_PIPER_BIN:opt/piper/piper}
tts.piper.model=${TTS_PIPER_MODEL:opt/piper/pt_BR-faber-medium.onnx}

app.open-browser=true
%test.app.open-browser=false
```

Os caminhos são relativos à pasta onde você executa o projeto. Para usar outro local, defina as variáveis `TTS_PIPER_BIN` e `TTS_PIPER_MODEL` ou passe `-Dtts.piper.bin=...` e `-Dtts.piper.model=...`.

### 3. Executar em modo desenvolvimento

```bash
./mvnw quarkus:dev
```

Abra `http://localhost:8080`. Com o live reload, as alterações em Java, HTML, SCSS e JS são aplicadas ao salvar.

### 4. Gerar o JAR

```bash
./mvnw clean package -Dquarkus.package.jar.type=uber-jar
java -jar target/VozQuark-1.0.0-SNAPSHOT-runner.jar
```

Execute o JAR a partir da pasta que contém `opt/piper`, ou informe os caminhos com as propriedades acima.

## API

### `POST /tts`

Converte um texto em áudio.

**Requisição**

```http
POST /tts
Content-Type: application/json

{ "text": "Olá, este é um teste do VozQuark." }
```

**Respostas**

| Status | Significado |
|---|---|
| `200` | Retorna o áudio (`audio/wav`) |
| `400` | Texto vazio ou fora do limite de 3 a 500 caracteres |
| `429` | Servidor ocupado ou limite de requisições excedido (header `Retry-After`) |
| `500` | Falha ao executar o Piper |

**Exemplo com curl**

```bash
curl -X POST http://localhost:8080/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Olá, este é um teste do VozQuark."}' \
  --output teste.wav
```

## Testes

```bash
./mvnw test
```

Os testes do `TtsResource` usam um mock do `TtsService`, então não dependem do Piper instalado. Eles rodam na porta 8081 (perfil de teste).

## Estrutura do projeto

```
VozQuark/
├── src/main/java/org/acme/
│   ├── TtsResource.java          # endpoint REST
│   ├── ExceptionMappers.java     # converte exceções em 429
│   ├── BrowserLauncher.java      # abre o navegador ao iniciar
│   ├── dto/SpeechRequest.java    # corpo da requisição e validações
│   └── service/TtsService.java   # executa o Piper
├── src/main/resources/
│   ├── web/                      # front-end (index.html, app/main.js, app/main.scss)
│   └── application.properties
├── src/test/                     # testes automatizados
└── opt/piper/                    # Piper e modelo de voz (não versionado)
```

## Tecnologias

- [Quarkus](https://quarkus.io) (REST, Hibernate Validator, SmallRye Fault Tolerance)
- [Quarkus Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/) (SCSS e JS)
- [Piper TTS](https://github.com/rhasspy/piper)
- JUnit 5, RestAssured e Mockito

## Solução de problemas

- **`Cannot run program ".../piper"`**: o caminho em `tts.piper.bin` está errado ou o Piper não foi baixado. Confira com `ls opt/piper/piper`.
- **Permissão negada no Linux**: rode `chmod +x opt/piper/piper`.
- **Abriu a Dev UI em vez da página**: confirme que o arquivo é `src/main/resources/web/index.html`, sem espaços no nome, e rode `./mvnw clean quarkus:dev`.
- **O navegador não abriu sozinho**: no Linux é necessário o `xdg-open`. Se faltar, acesse `http://localhost:8080` manualmente ou use `-Dapp.open-browser=false`.

## Créditos e licenças de terceiros

- **Piper TTS** ([rhasspy/piper](https://github.com/rhasspy/piper)): licença MIT.
- **espeak-ng** ([espeak-ng/espeak-ng](https://github.com/espeak-ng/espeak-ng)): licença GPL-3.0. Incluído como biblioteca nos pacotes do Piper, sem modificações.
- **Voz `pt_BR-faber-medium`** ([rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices)): dataset CC0 ([NabuCasa/voice-datasets](https://github.com/NabuCasa/voice-datasets)), modelo ajustado a partir da voz `lessac`.
- **Quarkus** ([quarkus.io](https://quarkus.io)): licença Apache 2.0.

Consulte os repositórios originais para os termos completos de cada licença.

## Autor

Feito por [lucastrindade222](https://github.com/lucastrindade222).

Quer entrar em contato? Acesse meu [portfólio](https://lucastrindade222.github.io/portfolio/).

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.