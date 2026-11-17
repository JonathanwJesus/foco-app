package com.foco.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Formatação de datas, horários e durações em português. */
object Fmt {
    val ptBR: Locale = Locale.forLanguageTag("pt-BR")

    /** Minutos desde 00:00 -> "14:30". */
    fun time(minutes: Int): String = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    /** Segundos -> "25:00". */
    fun clock(totalSec: Int): String = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)

    fun shortMonth(m: Month): String =
        m.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.').replaceFirstChar { it.uppercase() }

    fun shortWeekday(d: DayOfWeek): String =
        d.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.').replaceFirstChar { it.uppercase() }

    /** "Terça-feira, 10 de novembro". */
    fun longDate(d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)).replaceFirstChar { it.uppercase() }

    /** "10 nov". */
    fun shortDate(d: LocalDate): String = "${d.dayOfMonth} ${shortMonth(d.month).lowercase()}"

    /** 95 -> "1h 35min"; 40 -> "40 min". */
    fun duration(minutes: Int): String =
        if (minutes < 60) "$minutes min"
        else "${minutes / 60}h ${String.format(Locale.US, "%02d", minutes % 60)}min"

    /** Texto do prazo mostrado no cartão da tarefa. */
    fun dueText(task: Task, today: LocalDate): String {
        val d = LocalDate.ofEpochDay(task.dueDay)
        val t = time(task.dueMinutes)
        return when (d) {
            today -> t
            today.plusDays(1) -> "Amanhã $t"
            today.minusDays(1) -> "Ontem $t"
            else -> "${shortDate(d)} $t"
        }
    }
}

/** Cálculos de sequência de dias e estatísticas por período. */
object Stats {

    enum class Period(val label: String, val totalTitle: String) {
        WEEK("Semana", "Tempo de foco na semana"),
        MONTH("Mês", "Tempo de foco no mês"),
        SEMESTER("Semestre", "Tempo de foco nos últimos 6 meses"),
    }

    data class Bar(val label: String, val minutes: Int, val highlight: Boolean)

    data class PeriodStats(
        val bars: List<Bar>,
        val total: Int,
        val previous: Int,
        val bySubject: List<Pair<Subject, Int>>,
    ) {
        /** Variação percentual em relação ao período anterior (null se não há base de comparação). */
        val deltaPercent: Int?
            get() = if (previous <= 0) null else ((total - previous) * 100f / previous).toInt()
    }

    fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun minutesOn(sessions: List<FocusSession>, day: LocalDate): Int =
        sessions.filter { dayOf(it.endedAt) == day }.sumOf { it.minutes }

    fun sessionsOn(sessions: List<FocusSession>, day: LocalDate): Int =
        sessions.count { dayOf(it.endedAt) == day }

    /** Dias seguidos com pelo menos uma sessão (hoje ainda pode estar "em aberto"). */
    fun streak(sessions: List<FocusSession>, today: LocalDate): Int {
        val days = sessions.map { dayOf(it.endedAt) }.toSet()
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    private fun sumBetween(sessions: List<FocusSession>, from: LocalDate, to: LocalDate): Int =
        sessions.filter {
            val d = dayOf(it.endedAt)
            !d.isBefore(from) && !d.isAfter(to)
        }.sumOf { it.minutes }

    private data class Range(
        val from: LocalDate,
        val to: LocalDate,
        val prevFrom: LocalDate,
        val prevTo: LocalDate,
        val bars: List<Bar>,
    )

    fun compute(period: Period, sessions: List<FocusSession>, today: LocalDate): PeriodStats {
        val r = when (period) {
            Period.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Range(
                    from = start,
                    to = start.plusDays(6),
                    prevFrom = start.minusDays(7),
                    prevTo = start.minusDays(1),
                    bars = (0..6).map { i ->
                        val d = start.plusDays(i.toLong())
                        Bar(Fmt.shortWeekday(d.dayOfWeek), sumBetween(sessions, d, d), d == today)
                    },
                )
            }

            Period.MONTH -> {
                val first = today.withDayOfMonth(1)
                val last = today.with(TemporalAdjusters.lastDayOfMonth())
                val prevFirst = first.minusMonths(1)
                Range(
                    from = first,
                    to = last,
                    prevFrom = prevFirst,
                    prevTo = first.minusDays(1),
                    bars = listOf(1, 8, 15, 22, 29).filter { it <= last.dayOfMonth }.map { startDay ->
                        val s = first.withDayOfMonth(startDay)
                        val endDay = minOf(startDay + 6, last.dayOfMonth)
                        val e = first.withDayOfMonth(endDay)
                        Bar(
                            "$startDay-$endDay",
                            sumBetween(sessions, s, e),
                            !today.isBefore(s) && !today.isAfter(e),
                        )
                    },
                )
            }

            Period.SEMESTER -> {
                val current = YearMonth.from(today)
                val startMonth = current.minusMonths(5)
                Range(
                    from = startMonth.atDay(1),
                    to = current.atEndOfMonth(),
                    prevFrom = startMonth.minusMonths(6).atDay(1),
                    prevTo = startMonth.atDay(1).minusDays(1),
                    bars = (0..5).map { i ->
                        val ym = startMonth.plusMonths(i.toLong())
                        Bar(
                            Fmt.shortMonth(ym.month),
                            sumBetween(sessions, ym.atDay(1), ym.atEndOfMonth()),
                            ym == current,
                        )
                    },
                )
            }
        }

        val inRange = sessions.filter {
            val d = dayOf(it.endedAt)
            !d.isBefore(r.from) && !d.isAfter(r.to)
        }
        val bySubject = inRange
            .filter { it.subject != null }
            .groupBy { it.subject!! }
            .map { (subject, list) -> subject to list.sumOf { it.minutes } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        return PeriodStats(
            bars = r.bars,
            total = inRange.sumOf { it.minutes },
            previous = sumBetween(sessions, r.prevFrom, r.prevTo),
            bySubject = bySubject,
        )
    }
}
