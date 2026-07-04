# HCE Lab — Fase 1

## Escopo desta entrega

Isto implementa **apenas a Fase 1** do roadmap: HCE mínimo funcionando,
parser completo (formato curto, casos 1–4), response builder centralizado,
um protocolo (Lab Protocol) com máquina de estados explícita, logging, e
testes unitários que não dependem de dispositivo/emulador.

**Não incluído nesta entrega — de propósito:**

- Room / persistência de credenciais
- Retrofit / sincronização com servidor
- Android Keystore / AES / challenge-response / contador anti-replay
- Interface `ApduProtocol` para múltiplos protocolos

Motivo: cada um desses itens envolve decisões que dependem de coisas ainda
não validadas — a primeira delas sendo se o HCE dispara de fato no seu
dispositivo com este AID. Construir persistência e criptografia antes de
confirmar isso é arquitetura especulativa, não engenharia.

## Decisões de segurança documentadas

- **`requireDeviceUnlock="true"`** em `apduservice.xml`: o serviço só
  responde com o celular desbloqueado. Deixar isso em `false` por padrão,
  num protótipo de controle de acesso, seria uma falha sem justificativa.
- **AID `F0010203040506`**: uso privado (ISO/IEC 7816-5, categoria F0-FE),
  não corresponde a nenhum sistema real. Precisa bater exatamente entre
  `apduservice.xml` e `LabProtocol.AID_HEX`.
- **`ApduDispatcher` mede e loga o tempo de cada comando.** Se aparecer
  "ALERTA: processamento > 100ms" no Logcat, é sinal de risco real de
  timeout no leitor. O tempo de folga real depende do chipset NFC e do
  leitor (frames WTX do ISO 14443-4), não é garantido pela especificação
  — não ignore esse alerta quando Room/rede entrarem nas próximas fases.

## Limitação conhecida e aceita (não é bug)

`SessionManager` guarda a sessão só em memória. Se o Android matar o
processo do app entre dois comandos APDU (sob pressão de memória), a
sessão se perde e a transação reinicia do zero. Sem contador anti-replay
persistido ainda, isso é uma falha de robustez, não de segurança —
documentado em `Session.kt`. **Isso precisa ser revisitado quando entrar
anti-replay real (Fase 2):** contador tem que ir para Room, nunca ficar só
em memória.

## Como abrir o projeto

O repositório já é um projeto Gradle completo e autocontido (inclui
Gradle Wrapper — não é necessário ter Gradle instalado separadamente).

```bash
git clone https://github.com/GitHubPng/hce-lab.git
cd hce-lab
```

Abra a pasta no Android Studio ("Open") e deixe o Gradle sincronizar, ou
rode direto pela linha de comando:

```bash
./gradlew test          # testes unitários (JVM, sem emulador)
./gradlew assembleDebug # gera o APK debug em app/build/outputs/apk/debug/
```

### Nota sobre o Gradle Wrapper

`gradle-wrapper.jar` e os scripts `gradlew`/`gradlew.bat` foram obtidos
diretamente do repositório oficial `gradle/gradle` (tag `v8.7.0`), não
gerados localmente — meu ambiente de execução não tem acesso à distribuição
oficial do Gradle para gerar o wrapper do zero.

**Gap conhecido:** `gradle-wrapper.properties` não tem
`distributionSha256Sum` (verificação de integridade do download do
Gradle). Isso não foi omitido por descuido — eu não tinha como calcular o
hash correto sem baixar a distribuição de 8.7, e colocar um hash errado
quebraria o build silenciosamente pior do que não verificar. Se quiser
essa verificação, rode uma vez com internet local:
`./gradlew wrapper --gradle-version 8.7 --distribution-sha256-sum <hash>`
(o próprio Gradle calcula e preenche o hash certo).

## CI (GitHub Actions)

O workflow `.github/workflows/android-ci.yml` roda em todo push/PR para
`main`: instala JDK 17 e Android SDK, executa `./gradlew test`, publica o
relatório de testes como artifact, e depois gera e publica o APK debug
(`./gradlew assembleDebug`) como artifact. Não builda `release` — assinatura
de release é assunto de Fase futura, não faz sentido antes de existir
alguma funcionalidade real além do esqueleto de HCE.

## Testes unitários (sem hardware, sem emulador)

`ApduParserTest`, `ResponseApduTest` e `LabProtocolTest` validam parser e
máquina de estados isoladamente:

```bash
./gradlew :app:test
```

## Leitor NFC (reader-tool)

Este repositório tem **dois módulos**, para dois papéis diferentes:

- `app`: o cartão emulado (HCE) — `MyHostApduService` + Lab Protocol.
- `reader-tool`: um **leitor de tags NFC** com interface limpa. Começou
  como utilitário de depuração (mandava só o SELECT do Lab Protocol e
  mostrava hex cru numa TextView) e evoluiu para um leitor genérico.

**Por que um app próprio em vez de um app de terceiros ("NFC Tools" etc.):**
eu não controlo (nem verifiquei) se apps de terceiros realmente expõem
envio de APDU com AID customizado. Um leitor próprio elimina essa
incerteza — a mesma lógica de "nada de dependência não verificada" que
guiou as decisões anteriores deste projeto.

### O que o leitor mostra

Ao encostar qualquer tag NFC, o app entra em modo leitor e apresenta, em
cards (tema claro/escuro conforme o aparelho):

- **Resumo da tag**: tipo (MIFARE Classic/Ultralight/NTAG, ISO-DEP,
  NFC-A/B/F/V…), UID em hex, ATQA/SAK, tamanho máximo de transceive e a
  lista de tecnologias suportadas.
- **Conteúdo NDEF**: cada record decodificado — Texto (com idioma),
  URI/Link (tabela de prefixos do NFC Forum), MIME, tipo externo — além
  de tipo, ocupação e se a tag é gravável.
- **Lab Protocol (HCE)**: quando a tag é ISO-DEP, envia o SELECT do AID
  `F0010203040506` e mostra a resposta e o tempo — é assim que se valida
  o `MyHostApduService` do módulo `app`, agora dentro de uma leitura
  genérica de NFC.

A leitura de hardware e o parsing ficam em
`readertool/nfc/` (`NfcTagReader`, `NdefParser`); a `MainActivity` só
cuida de UI e do ciclo do `NfcAdapter` em modo leitor.

### Gravar dados em tags

No topo há um alternador **Ler / Gravar**. No modo **Gravar**:

1. Escolha o tipo: **Texto** (record NDEF RTD_TEXT) ou **Link / URL**
   (record NDEF RTD_URI — aceita `https://`, `tel:`, `mailto:`, etc.).
2. Digite o conteúdo, ou toque num dos **exemplos** para preencher.
3. Encoste uma tag NFC gravável — o app grava a mensagem NDEF e mostra o
   resultado (bytes gravados, ou o motivo da falha: tag protegida, sem
   espaço, ou que não suporta NDEF).

A gravação cobre tags já formatadas em NDEF (`Ndef`) e tags virgens
formatáveis (`NdefFormatable`, formatadas na hora). A lógica fica em
`readertool/nfc/NfcTagWriter`, espelhando o `NfcTagReader`. Depois de
gravar, volte para o modo **Ler** e encoste a mesma tag para conferir o
conteúdo.

### Testando o Lab Protocol (HCE) com dois celulares

1. Você precisa de **dois celulares Android com NFC**. Não precisam ser
   seus — pedir um celular de um colega por alguns minutos é suficiente.
2. No **celular A**: instale o APK do módulo `app` (artifact
   `app-debug-apk` do GitHub Actions, ou `./gradlew :app:installDebug`
   com o celular conectado via USB/ADB). Ative o NFC nas configurações.
3. No **celular B**: instale o APK do módulo `reader-tool` (artifact
   `reader-tool-debug-apk`, ou `./gradlew :reader-tool:installDebug`).
   Ative o NFC. Abra o app — ele já entra em modo leitor ao abrir.
4. Encoste as costas dos dois celulares (onde fica a antena NFC,
   geralmente perto da câmera traseira).
5. No celular B (o leitor), o card **Lab Protocol (HCE)** deve mostrar a
   resposta `90 00` e "SUCESSO — Lab Protocol respondeu corretamente".
6. Filtre o Logcat do **celular A** por `HceLab` para ver o lado do
   serviço (estado da sessão, tempo de processamento).

Você também pode encostar **qualquer tag NFC comum** (cartão de
transporte, etiqueta NTAG, crachá) no celular B para ver o resumo e o
conteúdo NDEF — sem precisar do celular A.

Se a tela do leitor não mostrar nada ao encostar: confira se o NFC está
ativo nos dois aparelhos, se o celular A não está bloqueado
(`requireDeviceUnlock="true"` exige desbloqueio), e se não há outro app
HCE instalado no celular A com o mesmo AID (ver seção de colisão de AID).

## Problema conhecido a testar: colisão de AID

Se você tiver outro app HCE instalado com o mesmo AID `F0010203040506`
(por exemplo, de outro protótipo seu), o Android abre um diálogo de
seleção de app no tap, o que quebra qualquer fluxo automatizado.
Desinstale apps HCE conflitantes antes de testar.

## Próximos passos (Fase 2 — não incluídos aqui)

- Modelar `Credential` (id, uidInterno, nome, nivelAcesso, ativo,
  expiraEm, ultimaSincronizacao) e persistir com Room — domínio único de
  controle de acesso, sem campos de cartão de pagamento.
- Gerar/armazenar chave simétrica no Android Keystore. Nunca em Room,
  nunca hardcoded.
- Implementar challenge-response real na transição
  `Selected -> Authenticated`, com contador anti-replay persistido.
- Extrair a interface `ApduProtocol` **só** quando existir um segundo
  protocolo concreto para validar o contrato — não antes.
