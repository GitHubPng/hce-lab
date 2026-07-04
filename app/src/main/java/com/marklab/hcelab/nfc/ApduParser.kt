package com.marklab.hcelab.nfc

/**
 * Parser de APDUs em formato curto (ISO/IEC 7816-4, §5.1).
 *
 * Regras aplicadas:
 *   4 bytes         -> Case 1: CLA INS P1 P2 (sem dados, sem Le)
 *   5 bytes         -> Case 2: CLA INS P1 P2 Le
 *   >5 bytes, Lc>0  -> Case 3 (sem Le) ou Case 4 (com Le), dependendo de
 *                      sobrar ou não 1 byte depois dos dados
 *
 * Formato ESTENDIDO não é tratado — ver CommandApdu.
 */
object ApduParser {

    class ApduParseException(message: String) : Exception(message)

    fun parse(raw: ByteArray?): CommandApdu {
        if (raw == null || raw.size < 4) {
            throw ApduParseException("APDU inválido: menor que 4 bytes")
        }

        val cla = raw[0]
        val ins = raw[1]
        val p1 = raw[2]
        val p2 = raw[3]

        if (raw.size == 4) {
            return CommandApdu(cla, ins, p1, p2)
        }

        if (raw.size == 5) {
            val leByte = raw[4].toInt() and 0xFF
            return CommandApdu(cla, ins, p1, p2, le = normalizeLe(leByte))
        }

        val lc = raw[4].toInt() and 0xFF
        if (lc == 0) {
            throw ApduParseException(
                "Lc=0 com bytes adicionais não é suportado neste parser (possível APDU estendido)"
            )
        }

        val dataStart = 5
        val dataEnd = dataStart + lc
        if (dataEnd > raw.size) {
            throw ApduParseException("Lc declarado ($lc) maior que os bytes disponíveis")
        }

        val data = raw.copyOfRange(dataStart, dataEnd)

        return when {
            dataEnd == raw.size -> CommandApdu(cla, ins, p1, p2, data = data)
            dataEnd + 1 == raw.size -> {
                val leByte = raw[dataEnd].toInt() and 0xFF
                CommandApdu(cla, ins, p1, p2, data = data, le = normalizeLe(leByte))
            }
            else -> throw ApduParseException(
                "Bytes sobrando após Data+Le: possível APDU estendido ou malformado"
            )
        }
    }

    // No formato curto, Le=0x00 significa "256 bytes esperados", não zero.
    private fun normalizeLe(leByte: Int): Int = if (leByte == 0) 256 else leByte
}
