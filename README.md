# VozQuark

Transforme texto em voz, direto no seu computador.

O **VozQuark** é um projeto de estudo feito com **Java** e **Quarkus** que converte texto em áudio (WAV) usando o [Piper TTS](https://github.com/rhasspy/piper), com voz em português do Brasil. Tudo roda localmente, sem enviar nada para a internet.

<!-- Adicione aqui uma captura de tela da interface:
![Tela do VozQuark](docs/screenshot.png)
-->

## Funcionalidades

- Interface web moderna (HTML, SCSS e JS) servida pelo próprio Quarkus
- Abre em uma **janela própria**, sem barra de endereço (usa o navegador instalado em modo app)
- Geração de áudio com a voz `pt_BR-faber-medium`
- Player integrado, download do `.wav` e botão **Salvar na pasta**
- Botão **Encerrar** com confirmação, que desliga o servidor
- Validação do texto (de 3 a 500 caracteres)
- Proteção contra sobrecarga: limite de execuções simultâneas e de requisições por minuto (respostas `429`)
- Servidor acessível apenas pela própria máquina (`127.0.0.1`)
- Testes automatizados com JUnit, RestAssured e Mockito

## Download

Baixe a versão pronta para o seu sistema na página de **[Releases](https://github.com/lucastrindade222/VozQuark/releases)**. Os pacotes já incluem o Java, o Piper e o modelo de voz.

| Sistema | Arquivo | Como executar |
|---|---|---|
| Windows | `VozQuark-windows.zip` | Extraia e abra `VozQuark\VozQuark.exe` |
| Linux | `VozQuark-linux.tar.gz` | `tar -xzf VozQuark-linux.tar.gz` e depois `./VozQuark/bin/VozQuark` |

### Requisito: um navegador baseado em Chromium

A janela do aplicativo é aberta com o navegador que já existe no computador, em modo app. É preciso ter um destes instalado:

- **Windows:** Microsoft Edge (já vem no sistema) ou Google Chrome
- **Linux:** Chromium, Google Chrome, Microsoft Edge ou Brave
- **macOS:** Google Chrome, Microsoft Edge ou Chromium

O Firefox não tem modo app. Se nenhum navegador compatível for encontrado, o programa continua rodando e o log mostra o endereço `http://localhost:8080` para abrir manualmente.

> **Aviso do Windows:** como o executável não é assinado, o SmartScreen pode mostrar "editor desconhecido". Clique em **Mais informações** e depois em **Executar assim mesmo**.

> Desenvolvido e testado em Linux. No Windows e no macOS a detecção do navegador foi escrita, mas ainda não foi validada.

### Como encerrar

- Clique no botão de desligar no canto superior direito do card e confirme, ou
- feche a janela do aplicativo (o servidor encerra junto).

## Como rodar a partir do código-fonte

### Requisitos

- JDK 25 (ou a versão definida no `pom.xml`)
- Piper e o modelo de voz (veja abaixo)
- Um navegador baseado em Chromium, para a janela (opcional em desenvolvimento)

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

O arquivo `src/main/resources/application.properties` reúne os caminhos e as opções:

| Propriedade | Padrão | Descrição |
|---|---|---|
| `quarkus.http.port` | `8080` | Porta do servidor |
| `quarkus.http.host` | `127.0.0.1` | Aceita conexões só da própria máquina |
| `tts.piper.bin` | `opt/piper/piper` | Caminho do executável do Piper |
| `tts.piper.model` | `opt/piper/pt_BR-faber-medium.onnx` | Caminho do modelo de voz |
| `tts.max-chars` | `500` | Tamanho máximo do texto |
| `tts.save-dir` | `audios` | Pasta onde o botão "Salvar na pasta" grava os arquivos |
| `app.window` | `true` (pacote) | Abre a janela do aplicativo ao iniciar |
| `app.open-browser` | `false` | Abre o navegador padrão (alternativa à janela) |

Os caminhos relativos valem a partir da pasta onde você executa o programa. Para usar outro local, passe `-Dtts.piper.bin=...` e `-Dtts.piper.model=...` ou defina as variáveis `TTS_PIPER_BIN` e `TTS_PIPER_MODEL`.

### 3. Executar em modo desenvolvimento

```bash
./mvnw quarkus:dev
```

No modo dev a janela fica desligada e a página abre no navegador normal. Com o live reload, as alterações em Java, HTML, SCSS e JS são aplicadas ao salvar. Para testar a janela, rode o JAR (passo seguinte) ou use `-Dapp.window=true`.

### 4. Gerar o JAR

```bash
./mvnw clean package -Dquarkus.package.jar.type=uber-jar
java -jar target/VozQuark-1.0.0-SNAPSHOT-runner.jar
```

Execute o JAR a partir da pasta que contém `opt/piper`, ou informe os caminhos com as propriedades acima.

## API

### `POST /tts`

Converte um texto em áudio.

```http
POST /tts
Content-Type: application/json

{ "text": "Olá, este é um teste do VozQuark." }
```

| Status | Significado |
|---|---|
| `200` | Retorna o áudio (`audio/wav`) |
| `400` | Texto vazio ou fora do limite de 3 a 500 caracteres |
| `415` | O corpo não é JSON |
| `429` | Servidor ocupado ou limite de requisições excedido (header `Retry-After`) |
| `500` | Falha ao executar o Piper |

```bash
curl -X POST http://localhost:8080/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Olá, este é um teste do VozQuark."}' \
  --output teste.wav
```

### `POST /audio/save`

Grava um WAV na pasta definida em `tts.save-dir`, com nome gerado pelo servidor, e devolve o caminho.

- Exige o header `X-Requested-With: VozQuark`
- Aceita apenas requisições vindas da própria máquina (`403` caso contrário)
- Só grava arquivos WAV válidos (`400` caso contrário)

### `POST /quit`

Encerra o servidor. Tem as mesmas proteções do `/audio/save` (header obrigatório e apenas `localhost`). É o que o botão **Encerrar** da interface chama.

## Testes

```bash
./mvnw test
```

Os testes do `TtsResource` usam um mock do `TtsService`, então não dependem do Piper instalado. Eles rodam na porta 8081 (perfil de teste), com a janela desativada.

## Principais classes

| Classe | Função |
|---|---|
| `TtsResource` | Endpoint `POST /tts` |
| `TtsService` | Executa o Piper e devolve o WAV (com limites de concorrência e taxa) |
| `SpeechRequest` | Corpo da requisição e validações |
| `AudioSaveResource` | Endpoint `POST /audio/save` |
| `QuitResource` | Endpoint `POST /quit` |
| `ExceptionMappers` | Converte as exceções de limite em `429` |
| `DesktopWindow` | Abre a janela do aplicativo e encerra o servidor quando ela fecha |
| `BrowserLauncher` | Alternativa que abre o navegador padrão |

O front-end fica em `src/main/resources/web/` (`index.html`, `app/main.js` e `app/main.scss`), compilado pelo Quarkus Web Bundler.

## Tecnologias

- [Quarkus](https://quarkus.io) (REST, Hibernate Validator, SmallRye Fault Tolerance)
- [Quarkus Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/) (SCSS e JS)
- [Piper TTS](https://github.com/rhasspy/piper)
- JUnit 5, RestAssured e Mockito

## Solução de problemas

- **`Cannot run program ".../piper"`**: o caminho em `tts.piper.bin` está errado ou o Piper não foi baixado. Confira com `ls opt/piper/piper`.
- **Permissão negada no Linux**: rode `chmod +x opt/piper/piper`.
- **Abriu a Dev UI em vez da página**: confirme que o arquivo é `src/main/resources/web/index.html`, sem espaços no nome, e rode `./mvnw clean quarkus:dev`.
- **Nenhuma janela abriu**: instale um navegador baseado em Chromium (veja acima) ou acesse `http://localhost:8080` manualmente.
- **A janela mostra uma versão antiga da página**: o navegador guarda cache no perfil do aplicativo. Apague `~/.vozquark/browser-profile` e abra de novo, ou use `Ctrl+Shift+R`.
- **`Port 8080 already in use`**: há outra instância rodando. Veja com `ss -ltnp | grep 8080` e encerre o processo.
- **O botão Encerrar não responde**: o `/quit` só aceita requisições locais. Confira se o acesso é por `localhost` e não pelo IP da rede.

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