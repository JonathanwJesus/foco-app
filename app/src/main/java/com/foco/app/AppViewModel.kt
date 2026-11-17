package com.foco.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foco.app.data.FocoRepository
import com.foco.app.data.FocusSession
import com.foco.app.data.Priority
import com.foco.app.data.Profile
import com.foco.app.data.Stats
import com.foco.app.data.Subject
import com.foco.app.data.Task
import com.foco.app.notifications.ReminderScheduler
import com.foco.app.timer.SystemTimeSource
import com.foco.app.timer.TimeSource
import com.foco.app.timer.TimerAlarm
import com.foco.app.timer.TimerMode
import com.foco.app.timer.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Eventos de uso único que a interface precisa tratar (navegação / avisos). */
sealed interface UiEvent {
    data object SessionFinished : UiEvent
    data object BreakFinished : UiEvent
}

/**
 * Estado e regras do app. Não depende do Android: o armazenamento, os lembretes, o alarme do
 * timer e o relógio entram pelo construtor, o que permite testar com versões falsas
 * (veja [appViewModelFactory] para a montagem real).
 */
class AppViewModel(
    private val repo: FocoRepository,
    private val reminders: ReminderScheduler,
    private val timerAlarm: TimerAlarm,
    private val timeSource: TimeSource = SystemTimeSource,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _tasks = MutableStateFlow(repo.loadTasks())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _sessions = MutableStateFlow(repo.loadSessions())
    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()

    private val _profile = MutableStateFlow(repo.loadProfile())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    private val _lastSession = MutableStateFlow<FocusSession?>(null)
    val lastSession: StateFlow<FocusSession?> = _lastSession.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var tickJob: Job? = null

    init {
        restoreTimer()
    }

    private fun now(): Long = timeSource.nowMillis()

    private fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone).toLocalDate()

    // =====================================================================
    // Conta (local, sem servidor)
    // =====================================================================

    fun hasAccount(): Boolean = _profile.value.passwordHash.isNotEmpty()

    fun signUp(name: String, email: String, password: String) {
        // uma nova conta neste aparelho substitui os dados anteriores
        wipeData()
        setProfile(
            Profile(
                name = name.trim(),
                email = email.trim().lowercase(),
                passwordHash = hash(password),
                loggedIn = true,
            )
        )
    }

    fun login(email: String, password: String): Boolean {
        val p = _profile.value
        val ok = p.passwordHash.isNotEmpty() &&
            p.email == email.trim().lowercase() &&
            p.passwordHash == hash(password)
        if (ok) setProfile(p.copy(loggedIn = true))
        return ok
    }

    fun logout() {
        stopTimer()
        setProfile(_profile.value.copy(loggedIn = false))
    }

    fun eraseAll() {
        stopTimer()
        wipeData()
        reminders.setDaily(false)
        _profile.value = Profile()
        repo.clearAll()
    }

    private fun wipeData() {
        _tasks.value.forEach { reminders.cancelTask(it.id) }
        _tasks.value = emptyList()
        _sessions.value = emptyList()
        _lastSession.value = null
        persistTasks()
        persistSessions()
    }

    private fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("foco:$password".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun setProfile(p: Profile) {
        _profile.value = p
        repo.saveProfile(p)
    }

    // =====================================================================
    // Preferências
    // =====================================================================

    fun setDailyReminder(enabled: Boolean) {
        setProfile(_profile.value.copy(dailyReminder = enabled))
        reminders.setDaily(enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) = setProfile(_profile.value.copy(keepScreenOn = enabled))

    fun setDailyGoal(goal: Int) = setProfile(_profile.value.copy(dailyGoal = goal.coerceIn(1, 12)))

    // =====================================================================
    // Tarefas
    // =====================================================================

    fun addTask(
        title: String,
        subject: Subject,
        day: LocalDate,
        minutes: Int,
        priority: Priority,
        reminder: Boolean,
    ) {
        val task = Task(
            id = now(),
            title = title.trim(),
            subject = subject,
            dueDay = day.toEpochDay(),
            dueMinutes = minutes,
            priority = priority,
            reminder = reminder,
        )
        _tasks.update { it + task }
        persistTasks()
        reminders.scheduleTask(task)
    }

    fun toggleTask(id: Long) {
        var changed: Task? = null
        _tasks.update { list ->
            list.map { t ->
                if (t.id == id) {
                    val done = !t.done
                    t.copy(done = done, doneAt = if (done) now() else null)
                        .also { changed = it }
                } else t
            }
        }
        persistTasks()
        changed?.let { if (it.done) reminders.cancelTask(it.id) else reminders.scheduleTask(it) }
    }

    fun deleteTask(id: Long) {
        _tasks.update { list -> list.filterNot { it.id == id } }
        persistTasks()
        reminders.cancelTask(id)
        if (_timer.value.taskId == id) updateTimer { it.withTask(null) }
    }

    private fun persistTasks() = repo.saveTasks(_tasks.value)
    private fun persistSessions() = repo.saveSessions(_sessions.value)

    // =====================================================================
    // Timer do modo foco
    //
    // A lógica de tempo fica em TimerState (guarda o horário em que o timer termina).
    // O estado é salvo a cada mudança e um alarme do sistema avisa o fim, então o timer
    // continua valendo com o app em segundo plano ou até fechado.
    // =====================================================================

    /** Troca o estado do timer e guarda no armazenamento. */
    private fun setTimer(state: TimerState) {
        _timer.value = state
        repo.saveTimer(state)
    }

    private fun updateTimer(block: (TimerState) -> TimerState) = setTimer(block(_timer.value))

    /** Ao abrir o app: retoma, ou conclui, o timer que estava salvo. */
    private fun restoreTimer() {
        val saved = repo.loadTimer()
        val t = now()
        when {
            !saved.running -> _timer.value = saved
            saved.finishedAt(t) -> {
                // terminou enquanto o app estava fechado: registra a sessão no horário em que acabou
                _timer.value = saved
                timerAlarm.cancel() // apaga o alarme e a notificação de fim, que o usuário já viu
                completeTimer(endedAt = saved.endsAtMillis ?: t)
            }
            else -> {
                _timer.value = saved.tick(t)
                timerAlarm.schedule(saved.endsAtMillis ?: t, saved.mode) // garante o alarme (ex.: depois de reiniciar o aparelho)
                startTicker()
            }
        }
    }

    fun selectTimerMode(mode: TimerMode) {
        if (_timer.value.running) return
        updateTimer { it.withMode(mode) }
    }

    fun pickTask(taskId: Long?) = updateTimer { it.withTask(taskId) }

    /** Abre o modo foco já com a tarefa escolhida (se não houver timer rodando). */
    fun focusOn(taskId: Long) {
        updateTimer { current ->
            if (current.running) current.withTask(taskId)
            else TimerState(mode = TimerMode.FOCUS, taskId = taskId)
        }
    }

    fun toggleTimer() {
        if (_timer.value.running) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        val started = _timer.value.start(now())
        val endsAt = started.endsAtMillis ?: return
        setTimer(started)
        timerAlarm.schedule(endsAt, started.mode)
        startTicker()
    }

    /** Atualiza a tela a cada instante e conclui o timer quando o horário de término chega. */
    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val t = now()
                _timer.update { it.tick(t) }
                if (_timer.value.finishedAt(t)) {
                    // sem cancelar o alarme: com o app em segundo plano é ele que mostra a notificação
                    completeTimer()
                    break
                }
            }
        }
    }

    private fun pauseTimer() {
        tickJob?.cancel()
        timerAlarm.cancel()
        updateTimer { it.pause(now()) }
    }

    fun resetTimer() {
        tickJob?.cancel()
        timerAlarm.cancel()
        updateTimer { it.reset() }
    }

    /** Botão "encerrar": para o timer e volta ao modo foco, sem registrar a sessão. */
    fun stopTimer() {
        tickJob?.cancel()
        timerAlarm.cancel()
        updateTimer { it.backToFocus() }
    }

    /** Atalho para demonstração: conclui o timer imediatamente como se tivesse chegado ao fim. */
    fun finishNow() {
        timerAlarm.cancel()
        completeTimer()
    }

    private fun completeTimer(endedAt: Long = now()) {
        tickJob?.cancel()
        val t = _timer.value
        if (t.mode == TimerMode.FOCUS) {
            val subject = t.taskId?.let { id -> _tasks.value.firstOrNull { it.id == id }?.subject }
            val session = FocusSession(
                id = endedAt,
                subject = subject,
                taskId = t.taskId,
                minutes = t.mode.minutes,
                endedAt = endedAt,
            )
            _sessions.update { it + session }
            persistSessions()
            _lastSession.value = session
            setTimer(t.backToFocus())
            _events.trySend(UiEvent.SessionFinished)
        } else {
            setTimer(t.backToFocus())
            _events.trySend(UiEvent.BreakFinished)
        }
    }

    /** Inicia a pausa (curta; a cada 4 sessões no dia, a longa). */
    fun startBreak() {
        val sessionsToday = Stats.sessionsOn(_sessions.value, today())
        val mode = if (sessionsToday > 0 && sessionsToday % 4 == 0) TimerMode.LONG else TimerMode.SHORT
        updateTimer { it.withMode(mode) }
        startTimer()
    }

    /** Marca como feita a tarefa da última sessão concluída (se ainda não estiver). */
    fun markLastSessionTaskDone() {
        val id = _lastSession.value?.taskId ?: return
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        if (!task.done) toggleTask(id)
    }

    // =====================================================================
    // Dados de exemplo (para demonstração)
    // =====================================================================

    private data class Sample(
        val title: String,
        val subject: Subject,
        val minutes: Int,
        val done: Boolean,
        val priority: Priority = Priority.MEDIUM,
    )

    fun loadSamples() {
        val now = now()
        val today = today()

        val defs = listOf(
            Sample("Resolver lista de derivadas", Subject.CALCULO, 14 * 60, false, Priority.HIGH),
            Sample("Resumo do capítulo 3", Subject.FISICA, 16 * 60 + 30, false),
            Sample("Rascunho da redação", Subject.REDACAO, 19 * 60, false),
            Sample("Ler capítulo de História", Subject.HISTORIA, 20 * 60 + 30, false),
            Sample("Revisar flashcards", Subject.FISICA, 9 * 60, true),
            Sample("Lista de exercícios 4", Subject.CALCULO, 8 * 60, true),
        )
        val newTasks = defs.mapIndexed { i, d ->
            Task(
                id = now + i,
                title = d.title,
                subject = d.subject,
                dueDay = today.toEpochDay(),
                dueMinutes = d.minutes,
                priority = d.priority,
                reminder = false,
                done = d.done,
                // concluídas "agora há pouco" (segundos atrás) para sempre contarem como feitas hoje
                doneAt = if (d.done) now - (i + 1) * 1_000L else null,
            )
        }
        _tasks.update { it + newTasks }
        persistTasks()

        // sessões de foco dos últimos 14 dias (índice = quantos dias atrás)
        val perDay = listOf(1, 2, 2, 3, 2, 0, 1, 2, 1, 3, 2, 2, 1, 0)
        val subjects = Subject.entries
        val sessions = mutableListOf<FocusSession>()
        var seq = 0L
        perDay.forEachIndexed { daysAgo, count ->
            repeat(count) { k ->
                val planned = today.minusDays(daysAgo.toLong())
                    .atTime(10 + k * 3, 0)
                    .atZone(zone).toInstant().toEpochMilli()
                val endedAt = if (planned > now) now - 60_000L * (k + 1) else planned
                sessions += FocusSession(
                    id = now + 1000 + seq++,
                    subject = subjects[(daysAgo + k) % subjects.size],
                    taskId = null,
                    minutes = 25,
                    endedAt = endedAt,
                )
            }
        }
        _sessions.update { it + sessions }
        persistSessions()
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
