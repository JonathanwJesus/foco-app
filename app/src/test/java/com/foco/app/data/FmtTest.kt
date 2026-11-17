package com.foco.app.data

import com.foco.app.testing.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FmtTest {

    private val today = LocalDate.of(2026, 11, 11)

    @Test
    fun `horario em minutos vira hh mm`() {
        assertEquals("00:00", Fmt.time(0))
        assertEquals("09:05", Fmt.time(9 * 60 + 5))
        assertEquals("14:30", Fmt.time(14 * 60 + 30))
        assertEquals("23:59", Fmt.time(23 * 60 + 59))
    }

    @Test
    fun `segundos viram mm ss`() {
        assertEquals("25:00", Fmt.clock(25 * 60))
        assertEquals("00:59", Fmt.clock(59))
        assertEquals("04:07", Fmt.clock(4 * 60 + 7))
    }

    @Test
    fun `duracao usa minutos ate uma hora e horas depois disso`() {
        assertEquals("0 min", Fmt.duration(0))
        assertEquals("40 min", Fmt.duration(40))
        assertEquals("1h 00min", Fmt.duration(60))
        assertEquals("1h 35min", Fmt.duration(95))
        assertEquals("12h 40min", Fmt.duration(12 * 60 + 40))
    }

    @Test
    fun `data curta e data longa em portugues`() {
        assertEquals("10 nov", Fmt.shortDate(LocalDate.of(2026, 11, 10)))
        assertEquals("Quarta-feira, 11 de novembro", Fmt.longDate(today))
    }

    @Test
    fun `prazo de hoje mostra so o horario`() {
        assertEquals("14:00", Fmt.dueText(task(day = today, minutes = 14 * 60), today))
    }

    @Test
    fun `prazo de amanha e de ontem trazem a palavra`() {
        assertEquals("Amanhã 09:00", Fmt.dueText(task(day = today.plusDays(1), minutes = 9 * 60), today))
        assertEquals("Ontem 09:00", Fmt.dueText(task(day = today.minusDays(1), minutes = 9 * 60), today))
    }

    @Test
    fun `outros dias trazem a data curta`() {
        assertEquals("15 nov 08:30", Fmt.dueText(task(day = LocalDate.of(2026, 11, 15), minutes = 8 * 60 + 30), today))
    }
}
