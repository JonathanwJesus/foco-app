package com.foco.app.data

import java.time.LocalDate
import java.time.LocalDateTime

/** Data e horário sugeridos ao criar uma tarefa. */
object DefaultDue {
    private const val LATE_HOUR = 22
    private const val MORNING_MINUTES = 9 * 60

    /**
     * Próxima hora cheia de hoje. A partir das 22h seria uma sugestão estranha (23:00 ou 00:00),
     * então a sugestão passa a ser amanhã às 09:00.
     *
     * @return o dia e o horário (em minutos desde 00:00)
     */
    fun suggest(now: LocalDateTime): Pair<LocalDate, Int> =
        if (now.hour >= LATE_HOUR) now.toLocalDate().plusDays(1) to MORNING_MINUTES
        else now.toLocalDate() to (now.hour + 1) * 60
}
