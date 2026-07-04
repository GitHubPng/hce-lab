package com.marklab.hcelab.nfc

/**
 * Builder de respostas APDU. Regra do projeto: nenhum lugar do código deve
 * escrever byteArrayOf(0x90, 0x00) diretamente — toda resposta passa por
 * aqui, para que o significado de cada status word fique explícito.
 */
object ResponseApdu {

    fun success(data: ByteArray = ByteArray(0)): ByteArray =
        data + StatusWord.SUCCESS

    fun fileNotFound(): ByteArray = StatusWord.FILE_NOT_FOUND

    fun securityStatusNotSatisfied(): ByteArray = StatusWord.SECURITY_STATUS_NOT_SATISFIED

    fun conditionsNotSatisfied(): ByteArray = StatusWord.CONDITIONS_NOT_SATISFIED

    fun instructionNotSupported(): ByteArray = StatusWord.INS_NOT_SUPPORTED

    fun classNotSupported(): ByteArray = StatusWord.CLA_NOT_SUPPORTED

    fun wrongLength(): ByteArray = StatusWord.WRONG_LENGTH

    fun unknownError(): ByteArray = StatusWord.UNKNOWN_ERROR
}
