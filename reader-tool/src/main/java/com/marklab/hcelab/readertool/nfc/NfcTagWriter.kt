package com.marklab.hcelab.readertool.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/**
 * Grava mensagens NDEF em tags. Contraparte de [NfcTagReader]: concentra
 * aqui toda a conversa com o hardware para escrita, de modo que a Activity
 * só decide "o quê" gravar, não "como".
 *
 * Estratégia de gravação:
 *   1. Se a tag já é NDEF ([Ndef]) e gravável → escreve direto.
 *   2. Se ainda não é NDEF mas é formatável ([NdefFormatable]) → formata
 *      já com a mensagem.
 *   3. Caso contrário, a tag não suporta NDEF e não há o que fazer.
 */
object NfcTagWriter {

    data class WriteResult(
        val success: Boolean,
        val message: String,
        val bytesWritten: Int
    )

    /** Cria uma mensagem NDEF de texto (RTD_TEXT). */
    fun textMessage(text: String, language: String = "pt"): NdefMessage =
        NdefMessage(arrayOf(NdefRecord.createTextRecord(language, text)))

    /**
     * Cria uma mensagem NDEF de URI (RTD_URI). Aceita http(s), tel:,
     * mailto:, etc. — createUri já aplica a tabela de prefixos do NFC Forum.
     */
    fun uriMessage(uri: String): NdefMessage =
        NdefMessage(arrayOf(NdefRecord.createUri(uri)))

    fun write(tag: Tag, message: NdefMessage): WriteResult {
        Ndef.get(tag)?.let { return writeToNdef(it, message) }
        NdefFormatable.get(tag)?.let { return formatAndWrite(it, message) }
        return WriteResult(
            success = false,
            message = "Esta tag não suporta NDEF, então não dá para gravar dados nela.",
            bytesWritten = 0
        )
    }

    private fun writeToNdef(ndef: Ndef, message: NdefMessage): WriteResult {
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                return WriteResult(false, "A tag está protegida (somente leitura).", 0)
            }
            val size = message.toByteArray().size
            if (size > ndef.maxSize) {
                return WriteResult(
                    false,
                    "O conteúdo tem $size bytes, mas a tag só comporta ${ndef.maxSize} bytes.",
                    0
                )
            }
            ndef.writeNdefMessage(message)
            WriteResult(true, "Gravado com sucesso ($size bytes).", size)
        } catch (e: Exception) {
            WriteResult(false, "Falha ao gravar: ${describe(e)}", 0)
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {
                // Fechar pode falhar se a tag saiu do campo — não mascara o resultado.
            }
        }
    }

    private fun formatAndWrite(formatable: NdefFormatable, message: NdefMessage): WriteResult {
        return try {
            formatable.connect()
            formatable.format(message)
            val size = message.toByteArray().size
            WriteResult(true, "Tag formatada e gravada ($size bytes).", size)
        } catch (e: Exception) {
            WriteResult(
                false,
                "Falha ao formatar/gravar: ${describe(e)}. " +
                    "Tags MIFARE Classic muitas vezes não formatam nestes aparelhos — " +
                    "use uma NTAG/Ultralight se possível.",
                0
            )
        } finally {
            try {
                formatable.close()
            } catch (_: Exception) {
                // Ver comentário em writeToNdef.
            }
        }
    }

    /**
     * Exceções de NFC (TagLostException, FormatException, IOException) muitas
     * vezes vêm com message nula. Sem isto, o usuário via só "null". Mostrar
     * o nome da classe já diz o que aconteceu (ex.: tag saiu do campo).
     */
    private fun describe(e: Exception): String {
        val msg = e.message
        return if (msg.isNullOrBlank()) e.javaClass.simpleName else "${e.javaClass.simpleName}: $msg"
    }
}
