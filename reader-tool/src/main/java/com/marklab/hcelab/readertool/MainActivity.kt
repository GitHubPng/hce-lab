package com.marklab.hcelab.readertool

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App leitor mínimo para validar o Lab Protocol (Fase 1 do hce-lab) sem
 * depender de um app de terceiros cujo suporte a APDU customizado eu não
 * controlo.
 *
 * Instale num SEGUNDO celular Android com NFC — este app é o LEITOR, não
 * o cartão. O cartão emulado (MyHostApduService) roda no primeiro celular,
 * no app principal deste repositório.
 *
 * Fluxo: ao aproximar do celular-cartão, envia o SELECT do AID do Lab
 * Protocol (F0010203040506) e mostra a resposta crua (hex) na tela.
 *
 * Este app existe só para depuração manual — não faz parte do produto,
 * não tem testes automatizados, e não deve crescer além disso sem
 * necessidade concreta.
 */
class MainActivity : Activity(), NfcAdapter.ReaderCallback {

    private lateinit var logView: TextView
    private var nfcAdapter: NfcAdapter? = null

    // 00 A4 04 00 07 F0 01 02 03 04 05 06 -> SELECT do AID F0010203040506
    // Precisa bater exatamente com LabProtocol.AID_HEX no app principal.
    private val selectApdu = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logView = findViewById(R.id.logView)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            appendLog("ERRO: este dispositivo não tem NFC. Use outro celular como leitor.")
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            appendLog("Tag detectada, mas não é ISO-DEP (ISO 14443-4) — não é o nosso HCE.")
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 5000

            val startNanos = System.nanoTime()
            val response = isoDep.transceive(selectApdu)
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

            val hexResponse = response.joinToString(" ") { "%02X".format(it) }
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            appendLog("[$timestamp] SELECT enviado -> resposta: $hexResponse (${elapsedMs}ms)")
            val isSuccess = response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()
            if (isSuccess) {
                appendLog("   -> SUCESSO (90 00). Lab Protocol respondeu corretamente.")
            } else {
                appendLog("   -> Resposta inesperada. Ver StatusWord.kt no app principal.")
            }
        } catch (e: Exception) {
            appendLog("Erro na transação: ${e.message}")
        } finally {
            try {
                isoDep.close()
            } catch (_: Exception) {
                // Ignorado de propósito: falha ao fechar não deve mascarar o resultado já logado.
            }
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread { logView.append("\n$message") }
    }
}
