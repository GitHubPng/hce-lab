package com.marklab.hcelab.readertool.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset

/**
 * Decodifica um NdefMessage em records legíveis, seguindo os RTDs do NFC
 * Forum (Text e URI) e os TNFs do ISO 28361 / NFC Data Exchange Format.
 *
 * Escopo: cobre os tipos que aparecem em tags do dia a dia (texto, link,
 * MIME, URI absoluta, tipo externo). Não tenta interpretar Smart Poster
 * aninhado campo a campo — mostra a URI principal via NdefRecord.toUri(),
 * que já resolve o caso comum.
 */
object NdefParser {

    // Tabela de prefixos de URI do NFC Forum (RTD URI, seção 3.2.2).
    private val URI_PREFIXES = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://",
        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
        "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
        "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
        "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
        "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
    )

    fun parse(message: NdefMessage): List<NdefRecordInfo> =
        message.records.map { parseRecord(it) }

    private fun parseRecord(record: NdefRecord): NdefRecordInfo {
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> parseWellKnown(record)
            NdefRecord.TNF_MIME_MEDIA -> {
                val mime = record.toMimeType() ?: "application/octet-stream"
                NdefRecordInfo(
                    kind = "MIME",
                    content = payloadAsTextOrHex(record.payload),
                    detail = mime
                )
            }
            NdefRecord.TNF_ABSOLUTE_URI -> NdefRecordInfo(
                kind = "URI/Link",
                content = String(record.type, Charsets.UTF_8),
                detail = "URI absoluta"
            )
            NdefRecord.TNF_EXTERNAL_TYPE -> NdefRecordInfo(
                kind = "Tipo externo",
                content = payloadAsTextOrHex(record.payload),
                detail = "urn:nfc:ext:" + String(record.type, Charsets.UTF_8)
            )
            NdefRecord.TNF_EMPTY -> NdefRecordInfo("Vazio", "(sem conteúdo)", null)
            else -> NdefRecordInfo(
                kind = "Desconhecido",
                content = HexUtils.toSpacedHex(record.payload),
                detail = "TNF=${record.tnf}"
            )
        }
    }

    private fun parseWellKnown(record: NdefRecord): NdefRecordInfo {
        return when {
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> decodeText(record)
            record.type.contentEquals(NdefRecord.RTD_URI) -> decodeUri(record)
            else -> {
                // Outros RTDs (Smart Poster, etc.): tenta a URI embutida.
                val uri = record.toUri()
                if (uri != null) {
                    NdefRecordInfo("URI/Link", uri.toString(), "via Smart Poster")
                } else {
                    NdefRecordInfo(
                        kind = "Well-Known",
                        content = payloadAsTextOrHex(record.payload),
                        detail = String(record.type, Charsets.UTF_8)
                    )
                }
            }
        }
    }

    private fun decodeText(record: NdefRecord): NdefRecordInfo {
        val payload = record.payload
        if (payload.isEmpty()) return NdefRecordInfo("Texto", "(vazio)", null)

        val status = payload[0].toInt()
        val isUtf16 = status and 0x80 != 0
        val langLength = status and 0x3F
        val charset: Charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8

        if (payload.size < 1 + langLength) {
            return NdefRecordInfo("Texto", "(malformado)", null)
        }
        val language = String(payload, 1, langLength, Charsets.US_ASCII)
        val textStart = 1 + langLength
        val text = String(payload, textStart, payload.size - textStart, charset)
        return NdefRecordInfo(
            kind = "Texto",
            content = text,
            detail = "idioma: $language"
        )
    }

    private fun decodeUri(record: NdefRecord): NdefRecordInfo {
        val payload = record.payload
        if (payload.isEmpty()) return NdefRecordInfo("URI/Link", "(vazio)", null)

        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIXES.getOrElse(prefixIndex) { "" }
        val rest = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return NdefRecordInfo(
            kind = "URI/Link",
            content = prefix + rest,
            detail = null
        )
    }

    /** Textual quando possível (UTF-8 imprimível); senão, hex. */
    private fun payloadAsTextOrHex(payload: ByteArray): String {
        if (payload.isEmpty()) return "(vazio)"
        val text = String(payload, Charsets.UTF_8)
        val printable = text.all { it == '\n' || it == '\r' || it == '\t' || !it.isISOControl() }
        return if (printable) text else HexUtils.toSpacedHex(payload)
    }
}
