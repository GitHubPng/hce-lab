package com.marklab.hcelab.nfc

/**
 * Representação estruturada de um comando APDU (ISO/IEC 7816-4).
 *
 * Suporta apenas APDUs em formato CURTO (Lc/Le de 1 byte).
 * Formato ESTENDIDO (Lc/Le de 3 bytes, usado para payloads grandes) NÃO é
 * suportado nesta fase. Se algum protocolo futuro precisar disso, tem que
 * ser adicionado explicitamente ao ApduParser — não assuma que já funciona.
 */
data class CommandApdu(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray = ByteArray(0),
    val le: Int? = null
) {
    val insUnsigned: Int get() = ins.toInt() and 0xFF
    val claUnsigned: Int get() = cla.toInt() and 0xFF

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandApdu) return false
        return cla == other.cla &&
            ins == other.ins &&
            p1 == other.p1 &&
            p2 == other.p2 &&
            data.contentEquals(other.data) &&
            le == other.le
    }

    override fun hashCode(): Int {
        var result = cla.toInt()
        result = 31 * result + ins.toInt()
        result = 31 * result + p1.toInt()
        result = 31 * result + p2.toInt()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (le ?: 0)
        return result
    }
}
