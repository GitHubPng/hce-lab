package com.marklab.hcelab.protocol.lab

import com.marklab.hcelab.nfc.ApduParser
import com.marklab.hcelab.nfc.ResponseApdu
import com.marklab.hcelab.nfc.Session
import com.marklab.hcelab.util.ApduLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LabProtocolTest {

    @Before
    fun setUp() {
        // Substitui o sink padrão (android.util.Log, que não existe em
        // testes JVM puros) para não depender de Robolectric só para
        // validar a máquina de estados.
        ApduLogger.sink = { /* silencia logs durante os testes */ }
    }

    @Test
    fun `select com AID correto muda estado para Selected e retorna sucesso`() {
        val session = Session(id = "test-session")
        val select = ApduParser.parse(
            byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
                0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
            )
        )

        val response = LabProtocol.process(session, select)

        assertArrayEquals(ResponseApdu.success(), response)
        assertEquals(LabState.Selected, session.state)
    }

    @Test
    fun `select com AID errado retorna fileNotFound e mantem Idle`() {
        val session = Session(id = "test-session")
        val select = ApduParser.parse(
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x02, 0xAA.toByte(), 0xBB.toByte())
        )

        val response = LabProtocol.process(session, select)

        assertArrayEquals(ResponseApdu.fileNotFound(), response)
        assertEquals(LabState.Idle, session.state)
    }

    @Test
    fun `comando antes do select retorna conditionsNotSatisfied`() {
        val session = Session(id = "test-session")
        val getData = ApduParser.parse(byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00))

        val response = LabProtocol.process(session, getData)

        assertArrayEquals(ResponseApdu.conditionsNotSatisfied(), response)
    }

    @Test
    fun `instrucao desconhecida apos select retorna instructionNotSupported`() {
        val session = Session(id = "test-session", state = LabState.Selected)
        val unknown = ApduParser.parse(byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00))

        val response = LabProtocol.process(session, unknown)

        assertArrayEquals(ResponseApdu.instructionNotSupported(), response)
    }
}
