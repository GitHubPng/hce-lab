package com.marklab.hcelab.protocol.lab

import com.marklab.hcelab.nfc.CommandApdu
import com.marklab.hcelab.nfc.ResponseApdu
import com.marklab.hcelab.nfc.Session
import com.marklab.hcelab.util.ApduLogger

/**
 * Implementação do "Lab Protocol": protocolo proprietário, criado apenas
 * para aprendizado, sem relação com protocolos de cartões reais.
 *
 * Escopo desta fase (Fase 1):
 *   - Reconhecer SELECT do AID do Lab Protocol -> Selected
 *   - Qualquer instrução antes do SELECT correto -> erro de estado
 *   - Instruções não implementadas -> INS_NOT_SUPPORTED
 *
 * Deliberadamente NÃO implementado nesta fase (Fase 2):
 *   - Autenticação / challenge-response
 *   - Consulta a credenciais (Room)
 *   - Qualquer coisa que dependa de rede ou Keystore
 *
 * Não existe interface ApduProtocol para múltiplos protocolos ainda.
 * Essa abstração será extraída quando (e se) existir um segundo protocolo
 * real — criar a interface agora, com um único caso concreto, seria
 * adivinhar o contrato certo sem informação suficiente.
 */
object LabProtocol {

    // AID de uso privado (categoria F0-FE, ISO/IEC 7816-5). Precisa bater
    // exatamente com o aid-filter em res/xml/apduservice.xml.
    private const val AID_HEX = "F0010203040506"

    private val SELECT_INS: Byte = 0xA4.toByte()

    fun process(session: Session, command: CommandApdu): ByteArray {
        return when (session.state) {
            LabState.Idle -> handleIdle(session, command)
            LabState.Selected -> handleSelected(session, command)
            LabState.Authenticated -> ResponseApdu.instructionNotSupported() // Fase 2
            LabState.Finished -> ResponseApdu.conditionsNotSatisfied()
        }
    }

    private fun handleIdle(session: Session, command: CommandApdu): ByteArray {
        if (command.ins != SELECT_INS) {
            ApduLogger.log(
                "Comando recebido em Idle sem SELECT prévio: INS=%02X".format(command.insUnsigned)
            )
            return ResponseApdu.conditionsNotSatisfied()
        }

        val aidHex = command.data.joinToString("") { "%02X".format(it) }
        if (aidHex != AID_HEX) {
            ApduLogger.log("SELECT com AID desconhecido: $aidHex")
            return ResponseApdu.fileNotFound()
        }

        session.state = LabState.Selected
        ApduLogger.log("AID selecionado com sucesso, sessão ${session.id} -> Selected")
        return ResponseApdu.success()
    }

    private fun handleSelected(session: Session, command: CommandApdu): ByteArray {
        // Fase 1: nenhuma instrução além do SELECT é suportada ainda.
        // Autenticação (Fase 2) entra exatamente aqui.
        ApduLogger.log(
            "Instrução não suportada em Selected: INS=%02X".format(command.insUnsigned)
        )
        return ResponseApdu.instructionNotSupported()
    }
}
