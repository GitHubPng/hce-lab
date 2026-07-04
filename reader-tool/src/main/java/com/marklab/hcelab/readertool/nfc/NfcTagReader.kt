package com.marklab.hcelab.readertool.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converte um [Tag] cru do Android em um [TagInfo] estruturado e pronto
 * para exibição. Concentra aqui toda a conversa com o hardware (connect /
 * transceive / read NDEF) para que a Activity não precise conhecer as
 * classes `android.nfc.tech.*`.
 *
 * Também roda o teste do Lab Protocol quando a tag é ISO-DEP: encostar o
 * leitor no celular que roda o MyHostApduService continua funcionando como
 * antes, agora dentro de uma leitura genérica de NFC.
 */
object NfcTagReader {

    // 00 A4 04 00 07 F0 01 02 03 04 05 06 -> SELECT do AID F0010203040506.
    // Precisa bater com LabProtocol.AID_HEX no módulo `app`.
    private val LAB_SELECT_APDU = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
    )

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun read(tag: Tag): TagInfo {
        val technologies = tag.techList.map { friendlyTechName(it) }
        val nfcA = NfcA.get(tag)

        return TagInfo(
            timestamp = timeFormat.format(Date()),
            uidHex = HexUtils.toSpacedHex(tag.id),
            uidBytes = tag.id.size,
            technologies = technologies,
            tagType = classifyTagType(tag),
            atqa = nfcA?.let { HexUtils.toSpacedHex(it.atqa) },
            sak = nfcA?.let { "%02X".format(it.sak.toInt() and 0xFF) },
            maxTransceiveLength = maxTransceiveLength(tag),
            ndef = readNdef(tag),
            labProtocol = readLabProtocol(tag)
        )
    }

    private fun readNdef(tag: Tag): NdefInfo? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
            val records = message?.let { NdefParser.parse(it) } ?: emptyList()
            NdefInfo(
                typeLabel = ndef.type ?: "NDEF",
                writable = ndef.isWritable,
                maxSizeBytes = ndef.maxSize,
                currentSizeBytes = message?.byteArrayLength ?: 0,
                records = records
            )
        } catch (e: Exception) {
            NdefInfo(
                typeLabel = "NDEF (falha na leitura)",
                writable = false,
                maxSizeBytes = 0,
                currentSizeBytes = 0,
                records = listOf(
                    NdefRecordInfo("Erro", e.message ?: "não foi possível ler o NDEF", null)
                )
            )
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {
                // Fechar pode falhar se a tag saiu do campo — não mascara o resultado.
            }
        }
    }

    private fun readLabProtocol(tag: Tag): LabProtocolResult? {
        val isoDep = IsoDep.get(tag) ?: return null
        return try {
            isoDep.connect()
            isoDep.timeout = 5000

            val startNanos = System.nanoTime()
            val response = isoDep.transceive(LAB_SELECT_APDU)
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

            val success = response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()

            LabProtocolResult(
                success = success,
                responseHex = HexUtils.toSpacedHex(response),
                elapsedMs = elapsedMs,
                note = if (success) {
                    "SUCESSO (90 00) — Lab Protocol respondeu corretamente."
                } else {
                    "Resposta inesperada — ver StatusWord.kt no módulo app."
                }
            )
        } catch (e: Exception) {
            LabProtocolResult(
                success = false,
                responseHex = "—",
                elapsedMs = 0,
                note = "Falha ao enviar SELECT: ${e.message}"
            )
        } finally {
            try {
                isoDep.close()
            } catch (_: Exception) {
                // Ver comentário em readNdef.
            }
        }
    }

    private fun maxTransceiveLength(tag: Tag): Int? {
        IsoDep.get(tag)?.let { return it.maxTransceiveLength }
        NfcA.get(tag)?.let { return it.maxTransceiveLength }
        NfcB.get(tag)?.let { return it.maxTransceiveLength }
        NfcF.get(tag)?.let { return it.maxTransceiveLength }
        NfcV.get(tag)?.let { return it.maxTransceiveLength }
        return null
    }

    private fun classifyTagType(tag: Tag): String {
        val techs = tag.techList
        return when {
            techs.contains(MifareClassic::class.java.name) -> "MIFARE Classic"
            techs.contains(MifareUltralight::class.java.name) -> "MIFARE Ultralight / NTAG"
            techs.contains(IsoDep::class.java.name) -> "ISO-DEP (ISO 14443-4)"
            techs.contains(NfcV::class.java.name) -> "NFC-V (ISO 15693)"
            techs.contains(NfcF::class.java.name) -> "NFC-F (FeliCa)"
            techs.contains(NfcB::class.java.name) -> "NFC-B (ISO 14443-4B)"
            techs.contains(NfcA::class.java.name) -> "NFC-A (ISO 14443-3A)"
            else -> "Tag NFC"
        }
    }

    private fun friendlyTechName(fullName: String): String {
        val short = fullName.substringAfterLast('.')
        return when (short) {
            "NfcA" -> "NFC-A"
            "NfcB" -> "NFC-B"
            "NfcF" -> "NFC-F"
            "NfcV" -> "NFC-V"
            "IsoDep" -> "IsoDep"
            "Ndef" -> "NDEF"
            "NdefFormatable" -> "NDEF (formatável)"
            "MifareClassic" -> "MIFARE Classic"
            "MifareUltralight" -> "MIFARE Ultralight"
            else -> short
        }
    }
}
