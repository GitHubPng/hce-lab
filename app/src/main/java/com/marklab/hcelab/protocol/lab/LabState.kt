package com.marklab.hcelab.protocol.lab

/**
 * Estados do Lab Protocol.
 *
 * Fase 1 usa apenas Idle e Selected. Authenticated e Finished já existem
 * na modelagem porque fazem parte do desenho da máquina de estados — mas
 * NENHUMA lógica de autenticação real (Fase 2: Keystore/AES/challenge-
 * response) está implementada ainda. Não confunda a existência do estado
 * com a existência da funcionalidade.
 */
sealed class LabState {
    object Idle : LabState()
    object Selected : LabState()
    object Authenticated : LabState()
    object Finished : LabState()
}
