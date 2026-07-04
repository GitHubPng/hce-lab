package com.marklab.hcelab.nfc

/**
 * Status words (SW1 SW2) do ISO/IEC 7816-4 usados neste projeto.
 * Lista NÃO exaustiva — adicione conforme o protocolo exigir, mas sempre
 * aqui, nunca como byteArrayOf(...) espalhado pelo código.
 */
object StatusWord {
    val SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val SECURITY_STATUS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())
    val CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
    val CLA_NOT_SUPPORTED = byteArrayOf(0x6E.toByte(), 0x00.toByte())
    val WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
    val UNKNOWN_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
}
