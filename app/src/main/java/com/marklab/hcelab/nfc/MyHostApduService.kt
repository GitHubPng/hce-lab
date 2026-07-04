package com.marklab.hcelab.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.marklab.hcelab.util.ApduLogger

class MyHostApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        ApduLogger.logApdu("APDU recebido", commandApdu ?: ByteArray(0))
        val response = ApduDispatcher.dispatch(commandApdu)
        ApduLogger.logApdu("APDU respondido", response)
        return response
    }

    override fun onDeactivated(reason: Int) {
        val reasonText = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Link perdido (afastou o celular do leitor)"
            DEACTIVATION_DESELECTED -> "Deselecionado pelo leitor"
            else -> "Motivo desconhecido ($reason)"
        }
        ApduLogger.log("Sessão desativada: $reasonText")
        SessionManager.clear()
    }
}
