package com.foco.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DefaultDueTest {

    private fun at(hour: Int, minute: Int = 30) = LocalDateTime.of(2026, 11, 11, hour, minute)

    @Test
    fun `de dia sugere a proxima hora cheia de hoje`() {
        assertEquals(LocalDate.of(2026, 11, 11) to 9 * 60, DefaultDue.suggest(at(8)))
        assertEquals(LocalDate.of(2026, 11, 11) to 15 * 60, DefaultDue.suggest(at(14, 59)))
        assertEquals(LocalDate.of(2026, 11, 11) to 0 * 60 + 60, DefaultDue.suggest(at(0, 5)))
    }

    @Test
    fun `ate as 21h ainda sugere hoje`() {
        assertEquals(LocalDate.of(2026, 11, 11) to 22 * 60, DefaultDue.suggest(at(21)))
    }

    @Test
    fun `a partir das 22h sugere amanha as 9h`() {
        val tomorrow = LocalDate.of(2026, 11, 12)
        assertEquals(tomorrow to 9 * 60, DefaultDue.suggest(at(22)))
        assertEquals(tomorrow to 9 * 60, DefaultDue.suggest(at(23, 59)))
    }

    @Test
    fun `a virada do mes e do ano funciona`() {
        val newYear = DefaultDue.suggest(LocalDateTime.of(2026, 12, 31, 23, 10))
        assertEquals(LocalDate.of(2027, 1, 1) to 9 * 60, newYear)
    }
}
