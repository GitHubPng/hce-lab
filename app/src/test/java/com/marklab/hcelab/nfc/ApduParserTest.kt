package com.marklab.hcelab.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ApduParserTest {

    @Test
    fun `case 1 - apenas cabecalho, sem dados nem Le`() {
        val raw = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val cmd = ApduParser.parse(raw)
        assertEquals(0x00.toByte(), cmd.cla)
        assertEquals(0xA4.toByte(), cmd.ins)
        assertEquals(0, cmd.data.size)
        assertNull(cmd.le)
    }

    @Test
    fun `case 2 - cabecalho com Le`() {
        val raw = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x10)
        val cmd = ApduParser.parse(raw)
        assertEquals(0x10, cmd.le)
    }

    @Test
    fun `case 3 - select com AID, sem Le`() {
        // 00 A4 04 00 07 F0010203040506
        val raw = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        )
        val cmd = ApduParser.parse(raw)
        assertEquals(7, cmd.data.size)
        assertNull(cmd.le)
        assertEquals(0xF0.toByte(), cmd.data[0])
    }

    @Test
    fun `case 4 - dados e Le`() {
        val raw = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x02, 0x01, 0x02, 0x00)
        val cmd = ApduParser.parse(raw)
        assertEquals(2, cmd.data.size)
        // Le=0x00 no formato curto significa 256, não zero
        assertEquals(256, cmd.le)
    }

    @Test
    fun `menor que 4 bytes deve lancar excecao`() {
        assertThrows(ApduParser.ApduParseException::class.java) {
            ApduParser.parse(byteArrayOf(0x00, 0xA4.toByte()))
        }
    }

    @Test
    fun `lc maior que bytes disponiveis deve lancar excecao`() {
        assertThrows(ApduParser.ApduParseException::class.java) {
            ApduParser.parse(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x05, 0x01))
        }
    }

    @Test
    fun `null deve lancar excecao de parsing, nao NPE`() {
        assertThrows(ApduParser.ApduParseException::class.java) {
            ApduParser.parse(null)
        }
    }
}
