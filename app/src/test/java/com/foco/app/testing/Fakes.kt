package com.foco.app.testing

import com.foco.app.data.FocoRepository
import com.foco.app.data.FocusSession
import com.foco.app.data.Priority
import com.foco.app.data.Profile
import com.foco.app.data.Subject
import com.foco.app.data.Task
import com.foco.app.notifications.ReminderScheduler
import com.foco.app.timer.TimeSource
import com.foco.app.timer.TimerAlarm
import com.foco.app.timer.TimerMode
import com.foco.app.timer.TimerState
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Armazenamento em memória: guarda os dados salvos para os testes conferirem. */
class InMemoryRepository(
    var tasks: List<Task> = emptyList(),
    var sessions: List<FocusSession> = emptyList(),
    var profile: Profile = Profile(),
    var timer: TimerState = TimerState(),
) : FocoRepository {
    var cleared = false

    override fun loadTasks(): List<Task> = tasks
    override fun saveTasks(tasks: List<Task>) { this.tasks = tasks }
    override fun loadSessions(): List<FocusSession> = sessions
    override fun saveSessions(sessions: List<FocusSession>) { this.sessions = sessions }
    override fun loadProfile(): Profile = profile
    override fun saveProfile(p: Profile) { profile = p }
    override fun loadTimer(): TimerState = timer
    override fun saveTimer(state: TimerState) { timer = state }
    override fun clearAll() {
        tasks = emptyList()
        sessions = emptyList()
        profile = Profile()
        timer = TimerState()
        cleared = true
    }
}

/** Alarme falso: anota quando o app pediu para agendar ou cancelar o aviso de fim do timer. */
class FakeTimerAlarm : TimerAlarm {
    val scheduled = mutableListOf<Pair<Long, TimerMode>>()
    var cancelCount = 0

    override fun schedule(endsAtMillis: Long, mode: TimerMode) { scheduled += endsAtMillis to mode }
    override fun cancel() { cancelCount++ }
}

/** Lembretes falsos: só anotam o que o app pediu para agendar ou cancelar. */
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Task>()
    val cancelled = mutableListOf<Long>()
    var dailyEnabled: Boolean? = null

    override fun scheduleTask(task: Task) { scheduled += task }
    override fun cancelTask(taskId: Long) { cancelled += taskId }
    override fun setDaily(enabled: Boolean) { dailyEnabled = enabled }
}

/** Relógio controlado pelo teste. Começa em 11/11/2026 às 12:00 (UTC), uma quarta-feira. */
class FakeTimeSource(var now: Long = START) : TimeSource {
    override fun nowMillis(): Long = now
    fun advance(ms: Long) { now += ms }

    companion object {
        val START: Long = ZonedDateTime.of(2026, 11, 11, 12, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant().toEpochMilli()
    }
}

/** Atalho para criar tarefas nos testes. */
fun task(
    id: Long = 1,
    title: String = "Tarefa",
    subject: Subject = Subject.CALCULO,
    day: LocalDate = LocalDate.of(2026, 11, 11),
    minutes: Int = 14 * 60,
    priority: Priority = Priority.MEDIUM,
    reminder: Boolean = false,
    done: Boolean = false,
    doneAt: Long? = null,
) = Task(
    id = id,
    title = title,
    subject = subject,
    dueDay = day.toEpochDay(),
    dueMinutes = minutes,
    priority = priority,
    reminder = reminder,
    done = done,
    doneAt = doneAt,
)

/** Atalho para criar sessões de foco nos testes (fuso do sistema, que os testes fixam em UTC). */
fun session(
    day: LocalDate,
    subject: Subject? = Subject.CALCULO,
    minutes: Int = 25,
    hour: Int = 12,
    id: Long = day.toEpochDay() * 100 + hour,
) = FocusSession(
    id = id,
    subject = subject,
    taskId = null,
    minutes = minutes,
    endedAt = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
)
