package com.marklab.hcelab.nfc

import com.marklab.hcelab.protocol.lab.LabProtocol
import com.marklab.hcelab.util.ApduLogger

/**
 * Ponto único de entrada entre o HostApduService e a lógica de protocolo.
 *
 * Hoje existe apenas um protocolo (LabProtocol), então este dispatcher é
 * deliberadamente simples: garante que existe uma sessão e mede o tempo de
 * processamento — importante porque processCommandApdu tem uma janela de
 * tempo curta antes do leitor dar timeout (ver README, seção sobre WTX).
 */
object ApduDispatcher {

    // Limiar de alerta, não um limite garantido pela especificação — o
    // tempo real disponível depende do leitor e do chipset NFC do celular.
    private const val WARNING_THRESHOLD_MS = 100

    fun dispatch(rawApdu: ByteArray?): ByteArray {
        val startTime = System.nanoTime()

        val response = try {
            val command = ApduParser.parse(rawApdu)
            val session = SessionManager.current() ?: SessionManager.startNewSession()
            LabProtocol.process(session, command)
        } catch (e: ApduParser.ApduParseException) {
            ApduLogger.log("Erro de parsing: ${e.message}")
            ResponseApdu.wrongLength()
        } catch (e: Exception) {
            ApduLogger.log("Erro inesperado no dispatch: ${e.message}")
            ResponseApdu.unknownError()
        }

        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
        ApduLogger.log("Processamento levou ${elapsedMs}ms")
        if (elapsedMs > WARNING_THRESHOLD_MS) {
            ApduLogger.log("ALERTA: processamento > ${WARNING_THRESHOLD_MS}ms, risco de timeout no leitor")
        }

        return response
    }
}
