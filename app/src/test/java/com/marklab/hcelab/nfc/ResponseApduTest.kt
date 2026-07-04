package com.marklab.hcelab.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ResponseApduTest {

    @Test
    fun `success sem dados retorna apenas 90 00`() {
        val result = ResponseApdu.success()
        assertArrayEquals(byteArrayOf(0x90.toByte(), 0x00.toByte()), result)
    }

    @Test
    fun `success com dados prefixa os dados antes do status word`() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val result = ResponseApdu.success(data)
        assertArrayEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x90.toByte(), 0x00.toByte()),
            result
        )
    }

    @Test
    fun `instructionNotSupported retorna 6D00`() {
        assertArrayEquals(
            byteArrayOf(0x6D.toByte(), 0x00.toByte()),
            ResponseApdu.instructionNotSupported()
        )
    }

    @Test
    fun `fileNotFound retorna 6A82`() {
        assertArrayEquals(
            byteArrayOf(0x6A.toByte(), 0x82.toByte()),
            ResponseApdu.fileNotFound()
        )
    }
}
