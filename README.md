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

## Como testar sem leitor físico

Rode `ApduParserTest`, `ResponseApduTest` e `LabProtocolTest` (JUnit4,
sem Android Instrumented Test, sem emulador). Eles validam parser e
máquina de estados isoladamente.

## Como testar com hardware

1. Instale o app num celular Android com NFC, API 24+.
2. Use outro celular com "NFC Tools" em modo leitor, ou um PN532 com
   `nfc-select F0010203040506`, para simular o SELECT.
3. Acompanhe o Logcat filtrando por `HceLab`.
4. Esperado: SELECT do AID correto → `90 00` + log "-> Selected".
   SELECT de AID errado → `6A 82`.

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
