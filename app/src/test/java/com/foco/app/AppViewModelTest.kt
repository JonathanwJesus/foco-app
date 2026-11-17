package com.foco.app

import com.foco.app.data.Priority
import com.foco.app.data.Profile
import com.foco.app.data.Stats
import com.foco.app.data.Subject
import com.foco.app.testing.FakeReminderScheduler
import com.foco.app.testing.FakeTimerAlarm
import com.foco.app.testing.FakeTimeSource
import com.foco.app.testing.InMemoryRepository
import com.foco.app.testing.MainDispatcherRule
import com.foco.app.testing.UtcTimeZoneRule
import com.foco.app.testing.session
import com.foco.app.testing.task
import com.foco.app.timer.TimerMode
import com.foco.app.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule(order = 0)
    val utc = UtcTimeZoneRule()

    @get:Rule(order = 1)
    val main = MainDispatcherRule()

    private val time = FakeTimeSource()
    private val reminders = FakeReminderScheduler()
    private val alarm = FakeTimerAlarm()

    /** Quarta-feira, 11/11/2026, o mesmo dia do relógio falso. */
    private val today = LocalDate.of(2026, 11, 11)

    private fun newVm(repo: InMemoryRepository = InMemoryRepository()) =
        AppViewModel(repo, reminders, alarm, time, ZoneOffset.UTC)

    /** Faz o tempo passar: avança o relógio falso e o relógio das corrotinas, em passos de 250 ms. */
    private fun TestScope.passTime(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(250L, left)
            time.advance(step)
            testScheduler.advanceTimeBy(step)
            testScheduler.runCurrent()
            left -= step
        }
    }

    private fun TestScope.collectEvents(vm: AppViewModel): List<UiEvent> {
        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events += it }
        }
        return events
    }

    // ------------------------------------------------------------------
    // Conta
    // ------------------------------------------------------------------

    @Test
    fun `cadastro cria o perfil logado e guarda o hash da senha, nao a senha`() {
        val repo = InMemoryRepository()
        val vm = newVm(repo)

        vm.signUp("  Ana Souza ", " Ana@Email.com ", "senha1234")

        val p = vm.profile.value
        assertTrue(p.loggedIn)
        assertEquals("Ana Souza", p.name)
        assertEquals("ana@email.com", p.email)
        assertEquals(64, p.passwordHash.length)
        assertNotEquals("senha1234", p.passwordHash)
        assertEquals(p, repo.profile)
        assertTrue(vm.hasAccount())
    }

    @Test
    fun `login aceita a senha certa e recusa a errada`() {
        val vm = newVm()
        vm.signUp("Ana", "ana@email.com", "senha1234")
        vm.logout()

        assertFalse(vm.profile.value.loggedIn)
        assertFalse(vm.login("ana@email.com", "errada"))
        assertFalse(vm.login("outra@email.com", "senha1234"))
        assertFalse(vm.profile.value.loggedIn)

        assertTrue(vm.login("  ANA@email.com ", "senha1234"))
        assertTrue(vm.profile.value.loggedIn)
    }

    @Test
    fun `sair mantem a conta e os dados`() {
        val vm = newVm()
        vm.signUp("Ana", "ana@email.com", "senha1234")
        vm.addTask("Estudar", Subject.CALCULO, today, 600, Priority.MEDIUM, false)

        vm.logout()

        assertTrue(vm.hasAccount())
        assertEquals(1, vm.tasks.value.size)
    }

    @Test
    fun `nova conta apaga os dados anteriores e cancela os lembretes`() {
        val repo = InMemoryRepository(
            tasks = listOf(task(id = 1), task(id = 2)),
            sessions = listOf(session(today)),
        )
        val vm = newVm(repo)

        vm.signUp("Ana", "ana@email.com", "senha1234")

        assertTrue(vm.tasks.value.isEmpty())
        assertTrue(vm.sessions.value.isEmpty())
        assertEquals(listOf(1L, 2L), reminders.cancelled)
    }

    @Test
    fun `apagar tudo limpa os dados, o perfil e o lembrete diario`() {
        val repo = InMemoryRepository()
        val vm = newVm(repo)
        vm.signUp("Ana", "ana@email.com", "senha1234")
        vm.setDailyReminder(true)

        vm.eraseAll()

        assertEquals(Profile(), vm.profile.value)
        assertTrue(repo.cleared)
        assertEquals(false, reminders.dailyEnabled)
        assertFalse(vm.hasAccount())
    }

    // ------------------------------------------------------------------
    // Preferências
    // ------------------------------------------------------------------

    @Test
    fun `meta diaria fica entre 1 e 12 sessoes`() {
        val vm = newVm()
        vm.setDailyGoal(0)
        assertEquals(1, vm.profile.value.dailyGoal)
        vm.setDailyGoal(50)
        assertEquals(12, vm.profile.value.dailyGoal)
        vm.setDailyGoal(6)
        assertEquals(6, vm.profile.value.dailyGoal)
    }

    @Test
    fun `lembrete diario liga e desliga no agendador`() {
        val vm = newVm()
        vm.setDailyReminder(true)
        assertTrue(vm.profile.value.dailyReminder)
        assertEquals(true, reminders.dailyEnabled)
        vm.setDailyReminder(false)
        assertEquals(false, reminders.dailyEnabled)
    }

    // ------------------------------------------------------------------
    // Tarefas
    // ------------------------------------------------------------------

    @Test
    fun `adicionar tarefa salva e agenda o lembrete`() {
        val repo = InMemoryRepository()
        val vm = newVm(repo)

        vm.addTask("  Resumo do capitulo  ", Subject.FISICA, LocalDate.of(2026, 11, 12), 9 * 60, Priority.HIGH, true)

        val t = vm.tasks.value.single()
        assertEquals("Resumo do capitulo", t.title)
        assertEquals(time.now, t.id)
        assertEquals(LocalDate.of(2026, 11, 12).toEpochDay(), t.dueDay)
        assertEquals(9 * 60, t.dueMinutes)
        assertEquals(Priority.HIGH, t.priority)
        assertFalse(t.done)
        assertEquals(listOf(t), repo.tasks)
        assertEquals(listOf(t), reminders.scheduled)
    }

    @Test
    fun `concluir tarefa marca a hora e cancela o lembrete, e desfazer agenda de novo`() {
        val vm = newVm()
        vm.addTask("Estudar", Subject.CALCULO, today, 15 * 60, Priority.MEDIUM, true)
        val id = vm.tasks.value.single().id

        time.advance(5_000)
        vm.toggleTask(id)
        val done = vm.tasks.value.single()
        assertTrue(done.done)
        assertEquals(time.now, done.doneAt)
        assertTrue(id in reminders.cancelled)

        vm.toggleTask(id)
        val undone = vm.tasks.value.single()
        assertFalse(undone.done)
        assertNull(undone.doneAt)
        assertEquals(2, reminders.scheduled.size)
    }

    @Test
    fun `excluir tarefa remove, cancela o lembrete e solta a tarefa do timer`() {
        val vm = newVm(InMemoryRepository(tasks = listOf(task(id = 7))))
        vm.focusOn(7)
        assertEquals(7L, vm.timer.value.taskId)

        vm.deleteTask(7)

        assertTrue(vm.tasks.value.isEmpty())
        assertNull(vm.timer.value.taskId)
        assertTrue(7L in reminders.cancelled)
    }

    // ------------------------------------------------------------------
    // Timer
    // ------------------------------------------------------------------

    @Test
    fun `a contagem regressiva anda com o tempo`() = runTest {
        val vm = newVm()

        vm.toggleTimer()
        assertTrue(vm.timer.value.running)
        assertEquals(1500, vm.timer.value.remainingSec)

        passTime(10_000)
        assertEquals(1490, vm.timer.value.remainingSec)

        vm.stopTimer()
    }

    @Test
    fun `pausar guarda o tempo e retomar continua dali`() = runTest {
        val vm = newVm()
        vm.toggleTimer()
        passTime(10_000)

        vm.toggleTimer() // pausa
        assertFalse(vm.timer.value.running)
        assertEquals(1490, vm.timer.value.remainingSec)

        time.advance(60_000) // um minuto parado
        assertEquals(1490, vm.timer.value.remainingSec)

        vm.toggleTimer() // retoma
        passTime(5_000)
        assertEquals(1485, vm.timer.value.remainingSec)

        vm.stopTimer()
    }

    @Test
    fun `ao chegar ao fim registra a sessao, avisa a tela e volta ao foco parado`() = runTest {
        val repo = InMemoryRepository(tasks = listOf(task(id = 5, subject = Subject.FISICA)))
        val vm = newVm(repo)
        val events = collectEvents(vm)
        vm.focusOn(5)

        vm.toggleTimer()
        passTime(25 * 60 * 1000L)

        val s = vm.sessions.value.single()
        assertEquals(25, s.minutes)
        assertEquals(Subject.FISICA, s.subject)
        assertEquals(5L, s.taskId)
        assertEquals(time.now, s.endedAt)
        assertEquals(listOf(s), repo.sessions)
        assertEquals(s, vm.lastSession.value)
        assertEquals(listOf<UiEvent>(UiEvent.SessionFinished), events)

        val t = vm.timer.value
        assertFalse(t.running)
        assertEquals(TimerMode.FOCUS, t.mode)
        assertEquals(1500, t.remainingSec)
        assertEquals(5L, t.taskId)
    }

    @Test
    fun `encerrar o timer nao registra sessao`() = runTest {
        val vm = newVm()
        vm.toggleTimer()
        passTime(10_000)

        vm.stopTimer()

        assertTrue(vm.sessions.value.isEmpty())
        assertFalse(vm.timer.value.running)
        assertEquals(1500, vm.timer.value.remainingSec)
    }

    @Test
    fun `reiniciar volta ao tempo cheio sem registrar sessao`() = runTest {
        val vm = newVm()
        vm.toggleTimer()
        passTime(30_000)

        vm.resetTimer()

        assertFalse(vm.timer.value.running)
        assertEquals(1500, vm.timer.value.remainingSec)
        assertTrue(vm.sessions.value.isEmpty())
    }

    @Test
    fun `so da para trocar o modo com o timer parado`() = runTest {
        val vm = newVm()
        vm.selectTimerMode(TimerMode.SHORT)
        assertEquals(TimerMode.SHORT, vm.timer.value.mode)
        assertEquals(300, vm.timer.value.remainingSec)

        vm.toggleTimer()
        vm.selectTimerMode(TimerMode.LONG)
        assertEquals(TimerMode.SHORT, vm.timer.value.mode)

        vm.stopTimer()
    }

    @Test
    fun `atalho de demonstracao conclui a sessao na hora`() = runTest {
        val vm = newVm()
        val events = collectEvents(vm)

        vm.finishNow()

        assertEquals(1, vm.sessions.value.size)
        assertEquals(listOf<UiEvent>(UiEvent.SessionFinished), events)
    }

    @Test
    fun `pausa curta termina sem registrar sessao e avisa a tela`() = runTest {
        val vm = newVm()
        val events = collectEvents(vm)

        vm.startBreak()
        assertEquals(TimerMode.SHORT, vm.timer.value.mode)
        assertTrue(vm.timer.value.running)

        passTime(5 * 60 * 1000L)

        assertTrue(vm.sessions.value.isEmpty())
        assertEquals(listOf<UiEvent>(UiEvent.BreakFinished), events)
        assertEquals(TimerMode.FOCUS, vm.timer.value.mode)
        assertFalse(vm.timer.value.running)
    }

    @Test
    fun `depois da quarta sessao do dia a pausa e a longa`() = runTest {
        val repo = InMemoryRepository(sessions = (1..3).map { session(today, hour = 7 + it) })
        val vm = newVm(repo)

        vm.finishNow() // quarta sessao de hoje
        vm.startBreak()

        assertEquals(TimerMode.LONG, vm.timer.value.mode)
        assertTrue(vm.timer.value.running)

        vm.stopTimer()
    }

    @Test
    fun `marcar a tarefa da ultima sessao como feita`() = runTest {
        val vm = newVm(InMemoryRepository(tasks = listOf(task(id = 9))))
        vm.focusOn(9)
        vm.finishNow()

        vm.markLastSessionTaskDone()

        assertTrue(vm.tasks.value.single().done)
    }

    // ------------------------------------------------------------------
    // Timer em segundo plano: alarme e estado salvo
    // ------------------------------------------------------------------

    @Test
    fun `iniciar agenda o alarme para o horario de termino e guarda o estado`() = runTest {
        val repo = InMemoryRepository()
        val vm = newVm(repo)

        vm.toggleTimer()

        assertEquals(listOf(time.now + 25 * 60 * 1000L to TimerMode.FOCUS), alarm.scheduled)
        assertTrue(repo.timer.running)
        assertEquals(time.now + 25 * 60 * 1000L, repo.timer.endsAtMillis)

        vm.stopTimer()
    }

    @Test
    fun `pausar cancela o alarme e guarda o tempo restante`() = runTest {
        val repo = InMemoryRepository()
        val vm = newVm(repo)
        vm.toggleTimer()
        passTime(10_000)

        vm.toggleTimer() // pausa

        assertEquals(1, alarm.cancelCount)
        assertFalse(repo.timer.running)
        assertEquals(1490, repo.timer.remainingSec)
    }

    @Test
    fun `retomar depois de pausar agenda um alarme novo`() = runTest {
        val vm = newVm()
        vm.toggleTimer()
        passTime(10_000)
        vm.toggleTimer() // pausa

        vm.toggleTimer() // retoma

        assertEquals(2, alarm.scheduled.size)
        assertEquals(time.now + 1490 * 1000L, alarm.scheduled.last().first)

        vm.stopTimer()
    }

    @Test
    fun `encerrar e reiniciar cancelam o alarme`() = runTest {
        val vm = newVm()
        vm.toggleTimer()
        vm.stopTimer()
        assertEquals(1, alarm.cancelCount)

        vm.toggleTimer()
        vm.resetTimer()
        assertEquals(2, alarm.cancelCount)
    }

    @Test
    fun `quando o timer termina sozinho o alarme fica, para avisar com o app em segundo plano`() = runTest {
        val vm = newVm()
        vm.toggleTimer()

        passTime(25 * 60 * 1000L)

        assertEquals(1, vm.sessions.value.size)
        assertEquals(0, alarm.cancelCount)
    }

    @Test
    fun `atalho de demonstracao cancela o alarme`() = runTest {
        val vm = newVm()
        vm.toggleTimer()

        vm.finishNow()

        assertEquals(1, alarm.cancelCount)
        assertEquals(1, vm.sessions.value.size)
        vm.stopTimer()
    }

    @Test
    fun `modo e tarefa escolhidos ficam guardados`() {
        val repo = InMemoryRepository()
        val vm = newVm(repo)

        vm.selectTimerMode(TimerMode.LONG)
        vm.pickTask(5)

        assertEquals(TimerMode.LONG, repo.timer.mode)
        assertEquals(5L, repo.timer.taskId)
    }

    @Test
    fun `ao reabrir com o timer rodando a contagem continua de onde estava`() = runTest {
        val repo = InMemoryRepository(timer = TimerState(taskId = 3).start(time.now))
        time.advance(60_000) // o app ficou fechado por um minuto

        val vm = newVm(repo)

        assertTrue(vm.timer.value.running)
        assertEquals(3L, vm.timer.value.taskId)
        assertEquals(1440, vm.timer.value.remainingSec)
        assertEquals(1, alarm.scheduled.size) // alarme garantido de novo

        passTime(10_000)
        assertEquals(1430, vm.timer.value.remainingSec)

        vm.stopTimer()
    }

    @Test
    fun `ao reabrir com o timer ja terminado registra a sessao no horario em que acabou`() = runTest {
        val started = time.now
        val repo = InMemoryRepository(timer = TimerState(taskId = null).start(started))
        time.advance(30 * 60 * 1000L) // o app ficou fechado por 30 minutos

        val vm = newVm(repo)
        val events = collectEvents(vm)

        val s = vm.sessions.value.single()
        assertEquals(started + 25 * 60 * 1000L, s.endedAt)
        assertEquals(25, s.minutes)
        assertEquals(listOf(s), repo.sessions)
        assertEquals(listOf<UiEvent>(UiEvent.SessionFinished), events)
        assertFalse(vm.timer.value.running)
        assertFalse(repo.timer.running)
        assertEquals(1, alarm.cancelCount)
    }

    @Test
    fun `ao reabrir com a pausa ja terminada avisa a tela sem registrar sessao`() = runTest {
        val repo = InMemoryRepository(timer = TimerState(mode = TimerMode.SHORT).start(time.now))
        time.advance(10 * 60 * 1000L)

        val vm = newVm(repo)
        val events = collectEvents(vm)

        assertTrue(vm.sessions.value.isEmpty())
        assertEquals(listOf<UiEvent>(UiEvent.BreakFinished), events)
        assertEquals(TimerMode.FOCUS, vm.timer.value.mode)
    }

    @Test
    fun `ao reabrir com o timer pausado mantem o tempo restante`() {
        val paused = TimerState().start(time.now).pause(time.now + 10_000)
        val vm = newVm(InMemoryRepository(timer = paused))

        assertFalse(vm.timer.value.running)
        assertEquals(1490, vm.timer.value.remainingSec)
        assertTrue(alarm.scheduled.isEmpty())
    }

    // ------------------------------------------------------------------
    // Dados de exemplo
    // ------------------------------------------------------------------

    @Test
    fun `dados de exemplo criam tarefas, duas ja concluidas hoje, e uma sequencia de cinco dias`() {
        val vm = newVm()

        vm.loadSamples()

        assertEquals(6, vm.tasks.value.size)
        val done = vm.tasks.value.filter { it.done }
        assertEquals(2, done.size)
        assertTrue(done.all { Stats.dayOf(it.doneAt!!) == today })
        assertEquals(5, Stats.streak(vm.sessions.value, today))
    }
}
