package com.marklab.hcelab.readertool.nfc

/**
 * Representação estruturada de uma tag NFC lida. É um dado imutável e
 * agnóstico de UI — quem monta a tela (MainActivity) recebe isto pronto,
 * a leitura de NFC (NfcTagReader) devolve isto. Separar assim mantém a
 * Activity sem lógica de parsing.
 */
data class TagInfo(
    val timestamp: String,
    val uidHex: String,
    val uidBytes: Int,
    val technologies: List<String>,
    val tagType: String,
    val atqa: String?,
    val sak: String?,
    val maxTransceiveLength: Int?,
    val ndef: NdefInfo?,
    val labProtocol: LabProtocolResult?
)

/** Um record NDEF já decodificado para exibição. */
data class NdefRecordInfo(
    val kind: String,      // "Texto", "URI/Link", "MIME", etc.
    val content: String,   // conteúdo já legível
    val detail: String?    // metadado opcional (idioma, tipo MIME, tamanho)
)

/** Bloco NDEF da tag, quando presente. */
data class NdefInfo(
    val typeLabel: String,     // ex.: "NFC Forum Type 2"
    val writable: Boolean,
    val maxSizeBytes: Int,
    val currentSizeBytes: Int,
    val records: List<NdefRecordInfo>
)

/** Resultado do teste do Lab Protocol (só para tags ISO-DEP). */
data class LabProtocolResult(
    val success: Boolean,
    val responseHex: String,
    val elapsedMs: Long,
    val note: String
)
