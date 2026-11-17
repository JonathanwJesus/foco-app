package com.foco.app.data

import com.foco.app.testing.UtcTimeZoneRule
import com.foco.app.testing.session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    @get:Rule
    val utc = UtcTimeZoneRule()

    /** Quarta-feira, 11/11/2026. */
    private val today = LocalDate.of(2026, 11, 11)

    // ------------------------------------------------------------------
    // Sequência de dias
    // ------------------------------------------------------------------

    @Test
    fun `sequencia e zero sem sessoes`() {
        assertEquals(0, Stats.streak(emptyList(), today))
    }

    @Test
    fun `sessao so hoje vale uma sequencia de um dia`() {
        assertEquals(1, Stats.streak(listOf(session(today)), today))
    }

    @Test
    fun `dias seguidos ate hoje somam a sequencia`() {
        val sessions = listOf(session(today), session(today.minusDays(1)), session(today.minusDays(2)))
        assertEquals(3, Stats.streak(sessions, today))
    }

    @Test
    fun `sequencia continua valendo se hoje ainda nao teve sessao`() {
        val sessions = listOf(session(today.minusDays(1)), session(today.minusDays(2)))
        assertEquals(2, Stats.streak(sessions, today))
    }

    @Test
    fun `sequencia zera se ontem tambem nao teve sessao`() {
        assertEquals(0, Stats.streak(listOf(session(today.minusDays(2))), today))
    }

    @Test
    fun `um dia sem sessao interrompe a sequencia`() {
        val sessions = listOf(session(today), session(today.minusDays(1)), session(today.minusDays(3)))
        assertEquals(2, Stats.streak(sessions, today))
    }

    @Test
    fun `varias sessoes no mesmo dia contam como um dia so`() {
        val sessions = listOf(session(today, hour = 9), session(today, hour = 14), session(today, hour = 20))
        assertEquals(1, Stats.streak(sessions, today))
    }

    // ------------------------------------------------------------------
    // Minutos e sessões do dia
    // ------------------------------------------------------------------

    @Test
    fun `minutos e sessoes do dia somam so aquele dia`() {
        val sessions = listOf(
            session(today, hour = 9),
            session(today, hour = 15, minutes = 50),
            session(today.minusDays(1)),
        )
        assertEquals(75, Stats.minutesOn(sessions, today))
        assertEquals(2, Stats.sessionsOn(sessions, today))
        assertEquals(0, Stats.minutesOn(sessions, today.plusDays(1)))
    }

    // ------------------------------------------------------------------
    // Estatísticas por período
    // ------------------------------------------------------------------

    @Test
    fun `semana comeca na segunda e destaca o dia de hoje`() {
        val sessions = listOf(
            session(LocalDate.of(2026, 11, 9)),
            session(LocalDate.of(2026, 11, 10), hour = 9),
            session(LocalDate.of(2026, 11, 10), hour = 15),
            session(today),
            session(LocalDate.of(2026, 11, 2), minutes = 100), // semana anterior
            session(LocalDate.of(2026, 11, 16)),               // semana seguinte: fica de fora
        )
        val stats = Stats.compute(Stats.Period.WEEK, sessions, today)

        assertEquals(listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"), stats.bars.map { it.label })
        assertEquals(listOf(25, 50, 25, 0, 0, 0, 0), stats.bars.map { it.minutes })
        assertEquals(listOf(false, false, true, false, false, false, false), stats.bars.map { it.highlight })
        assertEquals(100, stats.total)
        assertEquals(100, stats.previous)
        assertEquals(0, stats.deltaPercent)
    }

    @Test
    fun `divisao por disciplina soma os minutos e ordena do maior para o menor`() {
        val sessions = listOf(
            session(today, subject = Subject.FISICA, hour = 8),
            session(today, subject = Subject.CALCULO, hour = 9),
            session(today, subject = Subject.CALCULO, hour = 10),
            session(today, subject = null, hour = 11), // sem disciplina: conta no total, mas nao na divisao
        )
        val stats = Stats.compute(Stats.Period.WEEK, sessions, today)

        assertEquals(listOf(Subject.CALCULO to 50, Subject.FISICA to 25), stats.bySubject)
        assertEquals(100, stats.total)
    }

    @Test
    fun `mes divide em semanas de sete dias e o ultimo bloco vai ate o fim do mes`() {
        val sessions = listOf(
            session(LocalDate.of(2026, 11, 1)),
            session(LocalDate.of(2026, 11, 9)),
            session(today),
            session(LocalDate.of(2026, 11, 30)),
            session(LocalDate.of(2026, 10, 31)), // mes anterior
            session(LocalDate.of(2026, 10, 1)),  // mes anterior
        )
        val stats = Stats.compute(Stats.Period.MONTH, sessions, today)

        assertEquals(listOf("1-7", "8-14", "15-21", "22-28", "29-30"), stats.bars.map { it.label })
        assertEquals(listOf(25, 50, 0, 0, 25), stats.bars.map { it.minutes })
        assertEquals(listOf(false, true, false, false, false), stats.bars.map { it.highlight })
        assertEquals(100, stats.total)
        assertEquals(50, stats.previous)
        assertEquals(100, stats.deltaPercent)
    }

    @Test
    fun `fevereiro de 28 dias tem so quatro blocos`() {
        val stats = Stats.compute(Stats.Period.MONTH, emptyList(), LocalDate.of(2027, 2, 10))
        assertEquals(listOf("1-7", "8-14", "15-21", "22-28"), stats.bars.map { it.label })
    }

    @Test
    fun `semestre mostra os ultimos seis meses e compara com os seis anteriores`() {
        val sessions = listOf(
            session(LocalDate.of(2026, 6, 15)),
            session(LocalDate.of(2026, 8, 3), minutes = 50),
            session(LocalDate.of(2026, 11, 2)),
            session(LocalDate.of(2026, 5, 10)),   // seis meses anteriores
            session(LocalDate.of(2025, 11, 20)),  // antigo demais: fora dos dois periodos
        )
        val stats = Stats.compute(Stats.Period.SEMESTER, sessions, today)

        assertEquals(listOf("Jun", "Jul", "Ago", "Set", "Out", "Nov"), stats.bars.map { it.label })
        assertEquals(listOf(25, 0, 50, 0, 0, 25), stats.bars.map { it.minutes })
        assertEquals(listOf(false, false, false, false, false, true), stats.bars.map { it.highlight })
        assertEquals(100, stats.total)
        assertEquals(25, stats.previous)
        assertEquals(300, stats.deltaPercent)
    }

    @Test
    fun `variacao percentual pode ser negativa`() {
        val stats = Stats.PeriodStats(bars = emptyList(), total = 50, previous = 100, bySubject = emptyList())
        assertEquals(-50, stats.deltaPercent)
    }

    @Test
    fun `sem periodo anterior nao ha variacao para mostrar`() {
        val stats = Stats.PeriodStats(bars = emptyList(), total = 50, previous = 0, bySubject = emptyList())
        assertNull(stats.deltaPercent)
    }
}
