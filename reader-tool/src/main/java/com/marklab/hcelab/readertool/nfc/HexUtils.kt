package com.marklab.hcelab.readertool.nfc

/** Utilitários de formatação de bytes. Sem estado, sem dependências. */
object HexUtils {

    /** Ex.: [0x04, 0xA2] -> "04 A2". Retorna "—" para vazio/nulo. */
    fun toSpacedHex(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "—"
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    /** Ex.: [0x04, 0xA2] -> "04A2". */
    fun toHex(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
