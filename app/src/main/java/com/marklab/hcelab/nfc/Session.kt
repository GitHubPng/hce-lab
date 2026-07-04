package com.marklab.hcelab.nfc

import com.marklab.hcelab.protocol.lab.LabState

/**
 * Estado de uma sessão de comunicação NFC (uma aproximação do leitor = uma
 * sessão).
 *
 * LIMITAÇÃO CONHECIDA E ACEITA NESTA FASE:
 * HostApduService pode ser destruído e recriado pelo sistema Android entre
 * dois comandos APDU (ex: sob pressão de memória). Como esta sessão vive
 * apenas em memória (ver SessionManager), isso pode causar perda de estado
 * no meio de uma transação, sem aviso ao leitor.
 *
 * Para o Lab Protocol atual (sem contador anti-replay persistido), essa
 * limitação é aceitável: na pior hipótese a sessão reinicia em Idle e o
 * leitor recebe um erro de estado — não uma falha de segurança silenciosa.
 *
 * Se no futuro for implementado contador anti-replay (Fase 2), ele PRECISA
 * ser persistido (Room). Perder o contador em memória quebraria a proteção
 * contra replay, e isso não pode ser adiado "para depois" quando chegar lá.
 */
data class Session(
    val id: String,
    var state: LabState = LabState.Idle,
    val createdAtMillis: Long = System.currentTimeMillis()
)
