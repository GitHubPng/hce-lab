package com.marklab.hcelab.nfc

import java.util.UUID

/**
 * Gerenciador de sessão simples, em memória, para uma única sessão ativa
 * por vez — HCE só permite uma transação por vez com o leitor aproximado.
 *
 * Ver Session.kt para a limitação conhecida sobre morte de processo.
 */
object SessionManager {

    @Volatile
    private var currentSession: Session? = null

    fun startNewSession(): Session {
        val session = Session(id = UUID.randomUUID().toString())
        currentSession = session
        return session
    }

    fun current(): Session? = currentSession

    fun clear() {
        currentSession = null
    }
}
