package com.marklab.hcelab.util

/**
 * Logger centralizado para APDUs e eventos do protocolo.
 *
 * `sink` é injetável de propósito: por padrão usa android.util.Log (só
 * funciona em runtime Android), mas testes JVM puros podem substituir por
 * println ou por nada, sem precisar de Robolectric/instrumentação só para
 * validar parser e máquina de estados.
 *
 * Nesta fase só loga em Logcat — não grava em arquivo/banco. Se precisar
 * de logs persistentes para depuração com PN532/captura externa (Fase 3),
 * isso precisa ser adicionado explicitamente aqui.
 */
object ApduLogger {
    private const val TAG = "HceLab"

    var sink: (String) -> Unit = { android.util.Log.d(TAG, it) }

    fun log(message: String) = sink(message)

    fun logApdu(label: String, bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        sink("$label: $hex")
    }
}
