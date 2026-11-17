package com.foco.app.data

/** Disciplinas disponíveis no app (cada uma tem uma cor própria na interface). */
enum class Subject(val label: String) {
    CALCULO("Cálculo II"),
    FISICA("Física"),
    REDACAO("Redação"),
    HISTORIA("História");

    companion object {
        fun from(value: String): Subject =
            entries.firstOrNull { it.name == value || it.label == value } ?: CALCULO
    }
}

enum class Priority(val label: String) {
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta"),
}

data class Task(
    val id: Long,
    val title: String,
    val subject: Subject,
    /** Dia de entrega, em dias desde 1970 (LocalDate.toEpochDay()). */
    val dueDay: Long,
    /** Horário de entrega, em minutos desde 00:00. */
    val dueMinutes: Int,
    val priority: Priority,
    val reminder: Boolean,
    val done: Boolean = false,
    /** Momento da conclusão, em milissegundos. */
    val doneAt: Long? = null,
)

/** Uma sessão de foco concluída (usada nas estatísticas e na sequência de dias). */
data class FocusSession(
    val id: Long,
    val subject: Subject?,
    val taskId: Long?,
    val minutes: Int,
    /** Momento em que a sessão terminou, em milissegundos. */
    val endedAt: Long,
)

data class Profile(
    val name: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val loggedIn: Boolean = false,
    val dailyReminder: Boolean = false,
    val keepScreenOn: Boolean = true,
    /** Meta diária de sessões de foco. */
    val dailyGoal: Int = 4,
) {
    val firstName: String
        get() = name.trim().substringBefore(' ').ifBlank { "estudante" }
}
