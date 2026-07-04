package com.marklab.hcelab.readertool

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.marklab.hcelab.readertool.nfc.NdefInfo
import com.marklab.hcelab.readertool.nfc.NdefRecordInfo
import com.marklab.hcelab.readertool.nfc.NfcTagReader
import com.marklab.hcelab.readertool.nfc.TagInfo

/**
 * Leitor NFC do HCE Lab.
 *
 * Evoluiu do utilitário de depuração original (que só mandava um SELECT do
 * Lab Protocol e cuspia hex numa TextView) para um leitor de tags genérico:
 * lê UID, tecnologias, tipo, ATQA/SAK, mensagens NDEF (texto, link, MIME…)
 * e, quando a tag é ISO-DEP, ainda roda o teste do Lab Protocol — então
 * encostar no celular que roda o MyHostApduService continua funcionando.
 *
 * A Activity só cuida de UI e do ciclo do NfcAdapter em modo leitor. Toda a
 * leitura de hardware e o parsing vivem em [NfcTagReader] / NdefParser.
 */
class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    private lateinit var statusIcon: ImageView
    private lateinit var statusText: TextView
    private lateinit var statusHint: TextView
    private lateinit var resultsContainer: LinearLayout
    private lateinit var clearButton: ImageButton

    private var startupFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Rede de segurança: se a inflação da tela ou o setup falhar, mostra
        // o erro NA TELA em vez de fechar o app sem aviso. Assim dá para
        // diagnosticar o problema mesmo sem acesso ao Logcat do aparelho.
        try {
            setContentView(R.layout.activity_main)

            statusIcon = findViewById(R.id.statusIcon)
            statusText = findViewById(R.id.statusText)
            statusHint = findViewById(R.id.statusHint)
            resultsContainer = findViewById(R.id.resultsContainer)
            clearButton = findViewById(R.id.clearButton)

            clearButton.setOnClickListener { reset() }

            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter == null) {
                showNoNfc()
            }
        } catch (t: Throwable) {
            startupFailed = true
            Log.e("HceLabReader", "Falha ao iniciar a tela", t)
            showFatal(t)
        }
    }

    private fun showFatal(t: Throwable) {
        val message = "Falha ao abrir o leitor.\n\n" +
            "${t.javaClass.simpleName}: ${t.message}\n\n" +
            Log.getStackTraceString(t)
        val text = TextView(this).apply {
            text = message
            setTextIsSelectable(true)
            setPadding(dp(20), dp(24), dp(20), dp(24))
            textSize = 13f
        }
        val scroll = android.widget.ScrollView(this).apply { addView(text) }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (startupFailed) return
        // Modo leitor: cobre NFC-A/B/F/V. Não usamos SKIP_NDEF para que o
        // sistema já resolva o NDEF durante o dispatch (cachedNdefMessage).
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    /** Roda numa thread do sistema, não na main. */
    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread { showReading() }
        val info = NfcTagReader.read(tag)
        runOnUiThread { render(info) }
    }

    // --- Estados da UI ---------------------------------------------------

    private fun showNoNfc() {
        statusIcon.setImageResource(R.drawable.ic_error)
        statusText.setText(R.string.status_no_nfc)
        statusHint.setText(R.string.status_no_nfc_hint)
    }

    private fun showReading() {
        statusIcon.setImageResource(R.drawable.ic_nfc)
        statusText.setText(R.string.status_reading)
        statusHint.setText(R.string.status_waiting_hint)
    }

    private fun reset() {
        resultsContainer.removeAllViews()
        clearButton.visibility = View.GONE
        statusIcon.setImageResource(R.drawable.ic_nfc)
        statusText.setText(R.string.status_waiting)
        statusHint.setText(R.string.status_waiting_hint)
    }

    private fun render(info: TagInfo) {
        vibrate()
        statusIcon.setImageResource(R.drawable.ic_check_circle)
        statusText.setText(R.string.status_done)
        statusHint.text = getString(R.string.header_subtitle)
        clearButton.visibility = View.VISIBLE

        resultsContainer.removeAllViews()
        renderSummary(info)
        info.ndef?.let { renderNdef(it) }
        info.labProtocol?.let { renderLab(it) }
    }

    // --- Cards -----------------------------------------------------------

    private fun renderSummary(info: TagInfo) {
        val body = addSectionCard(getString(R.string.section_summary), R.drawable.ic_tag)
        addKvRow(body, getString(R.string.label_type), info.tagType)
        addKvRow(body, getString(R.string.label_uid), "${info.uidHex}  (${info.uidBytes} bytes)")
        info.atqa?.let { addKvRow(body, getString(R.string.label_atqa), it) }
        info.sak?.let { addKvRow(body, getString(R.string.label_sak), it) }
        info.maxTransceiveLength?.let {
            addKvRow(body, getString(R.string.label_max_transceive), "$it bytes")
        }
        addChipsRow(body, getString(R.string.label_techs), info.technologies)
    }

    private fun renderNdef(ndef: NdefInfo) {
        val body = addSectionCard(getString(R.string.section_ndef), R.drawable.ic_link)
        addKvRow(body, getString(R.string.label_type), ndef.typeLabel)
        addKvRow(
            body,
            "Ocupação",
            "${ndef.currentSizeBytes} / ${ndef.maxSizeBytes} bytes" +
                if (ndef.writable) "  · gravável" else "  · somente leitura"
        )
        if (ndef.records.isEmpty()) {
            addKvRow(body, "Records", "Nenhum record NDEF")
        } else {
            ndef.records.forEach { addNdefRecord(body, it) }
        }
    }

    private fun renderLab(lab: com.marklab.hcelab.readertool.nfc.LabProtocolResult) {
        val body = addSectionCard(
            getString(R.string.section_lab),
            if (lab.success) R.drawable.ic_check_circle else R.drawable.ic_error
        )
        addKvRow(body, "Resposta", lab.responseHex)
        if (lab.elapsedMs > 0) addKvRow(body, "Tempo", "${lab.elapsedMs} ms")
        val note = TextView(this).apply {
            text = lab.note
            textSize = 14f
            setTextColor(colorOnSurfaceVariant())
            setPadding(0, dp(6), 0, 0)
        }
        body.addView(note)
    }

    // --- Helpers de construção de views ---------------------------------

    private fun addSectionCard(title: String, iconRes: Int): LinearLayout {
        val card = LayoutInflater.from(this)
            .inflate(R.layout.view_section_card, resultsContainer, false)
        card.findViewById<TextView>(R.id.sectionTitle).text = title
        card.findViewById<ImageView>(R.id.sectionIcon).setImageResource(iconRes)
        resultsContainer.addView(card)
        return card.findViewById(R.id.sectionBody)
    }

    private fun addKvRow(parent: LinearLayout, label: String, value: String) {
        val row = LayoutInflater.from(this).inflate(R.layout.view_kv_row, parent, false)
        row.findViewById<TextView>(R.id.rowLabel).text = label
        row.findViewById<TextView>(R.id.rowValue).text = value
        parent.addView(row)
    }

    private fun addChipsRow(parent: LinearLayout, label: String, values: List<String>) {
        val labelView = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(colorOnSurfaceVariant())
            setPadding(0, dp(8), 0, dp(6))
        }
        parent.addView(labelView)

        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        values.forEach { tech ->
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.view_chip, strip, false) as TextView
            chip.text = tech
            (chip.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp(8)
            strip.addView(chip)
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(strip)
        }
        parent.addView(scroll)
    }

    private fun addNdefRecord(parent: LinearLayout, record: NdefRecordInfo) {
        val view = LayoutInflater.from(this).inflate(R.layout.view_ndef_record, parent, false)
        view.findViewById<TextView>(R.id.recordKind).text = record.kind
        view.findViewById<TextView>(R.id.recordContent).text = record.content
        val detail = view.findViewById<TextView>(R.id.recordDetail)
        if (record.detail.isNullOrBlank()) {
            detail.visibility = View.GONE
        } else {
            detail.text = record.detail
        }
        parent.addView(view)
    }

    // --- Utilidades ------------------------------------------------------

    private fun colorOnSurfaceVariant(): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, tv, true
        )
        return tv.data
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(40)
        }
    }
}
