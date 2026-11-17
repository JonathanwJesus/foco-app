package com.foco.app.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    private val t0 = 1_000_000L

    @Test
    fun `estado inicial esta parado com o tempo cheio do modo`() {
        val s = TimerState()
        assertFalse(s.running)
        assertNull(s.endsAtMillis)
        assertEquals(TimerMode.FOCUS, s.mode)
        assertEquals(25 * 60, s.remainingSec)
        assertEquals(1f, s.progress, 0.0001f)
        assertEquals(5 * 60, TimerState(mode = TimerMode.SHORT).remainingSec)
        assertEquals(15 * 60, TimerState(mode = TimerMode.LONG).remainingSec)
    }

    @Test
    fun `start define o horario de termino e marca como rodando`() {
        val s = TimerState().start(t0)
        assertTrue(s.running)
        assertEquals(t0 + 25 * 60 * 1000L, s.endsAtMillis)
    }

    @Test
    fun `start com o timer rodando nao muda o horario de termino`() {
        val started = TimerState().start(t0)
        val again = started.start(t0 + 5_000)
        assertSame(started, again)
    }

    @Test
    fun `start sem tempo restante nao faz nada`() {
        val s = TimerState(remainingSec = 0).start(t0)
        assertFalse(s.running)
    }

    @Test
    fun `tempo restante arredonda para cima e so cai depois de um segundo inteiro`() {
        val s = TimerState().start(t0)
        assertEquals(1500, s.remainingAt(t0))
        assertEquals(1500, s.remainingAt(t0 + 1))
        assertEquals(1500, s.remainingAt(t0 + 999))
        assertEquals(1499, s.remainingAt(t0 + 1_000))
        assertEquals(1, s.remainingAt(t0 + 1_499_999))
        assertEquals(0, s.remainingAt(t0 + 1_500_000))
    }

    @Test
    fun `tempo restante nunca fica negativo`() {
        val s = TimerState().start(t0)
        assertEquals(0, s.remainingAt(t0 + 9_999_999))
    }

    @Test
    fun `finishedAt so e verdadeiro a partir do horario de termino`() {
        val s = TimerState().start(t0)
        assertFalse(s.finishedAt(t0 + 1_499_999))
        assertTrue(s.finishedAt(t0 + 1_500_000))
        assertTrue(s.finishedAt(t0 + 2_000_000))
    }

    @Test
    fun `timer parado nunca esta terminado`() {
        assertFalse(TimerState().finishedAt(Long.MAX_VALUE))
    }

    @Test
    fun `pause guarda o tempo restante e para de rodar`() {
        val paused = TimerState().start(t0).pause(t0 + 10_000)
        assertFalse(paused.running)
        assertNull(paused.endsAtMillis)
        assertEquals(1490, paused.remainingSec)
    }

    @Test
    fun `retomar depois de pausar continua de onde parou`() {
        val paused = TimerState().start(t0).pause(t0 + 10_000)
        val resumed = paused.start(t0 + 600_000)
        assertTrue(resumed.running)
        assertEquals(t0 + 600_000 + 1490 * 1000L, resumed.endsAtMillis)
        assertEquals(1485, resumed.remainingAt(t0 + 605_000))
    }

    @Test
    fun `pause com o timer parado nao muda nada`() {
        val s = TimerState()
        assertSame(s, s.pause(t0))
    }

    @Test
    fun `tick atualiza o tempo restante e o progresso`() {
        val s = TimerState().start(t0).tick(t0 + 750_000)
        assertEquals(750, s.remainingSec)
        assertEquals(0.5f, s.progress, 0.0001f)
        assertTrue(s.running)
    }

    @Test
    fun `tick com o timer parado nao muda nada`() {
        val s = TimerState()
        assertSame(s, s.tick(t0 + 750_000))
    }

    @Test
    fun `reset volta ao tempo cheio parado e mantem modo e tarefa`() {
        val s = TimerState(mode = TimerMode.SHORT, taskId = 7).start(t0).tick(t0 + 100_000).reset()
        assertFalse(s.running)
        assertEquals(TimerMode.SHORT, s.mode)
        assertEquals(7L, s.taskId)
        assertEquals(300, s.remainingSec)
    }

    @Test
    fun `withMode troca o modo parado com o tempo cheio e mantem a tarefa`() {
        val s = TimerState(taskId = 3).start(t0).withMode(TimerMode.LONG)
        assertFalse(s.running)
        assertEquals(TimerMode.LONG, s.mode)
        assertEquals(900, s.remainingSec)
        assertEquals(3L, s.taskId)
    }

    @Test
    fun `backToFocus volta ao modo foco parado mantendo a tarefa`() {
        val s = TimerState(mode = TimerMode.SHORT, taskId = 4).start(t0).backToFocus()
        assertFalse(s.running)
        assertEquals(TimerMode.FOCUS, s.mode)
        assertEquals(1500, s.remainingSec)
        assertEquals(4L, s.taskId)
    }

    @Test
    fun `withTask so troca a tarefa`() {
        val running = TimerState().start(t0)
        val s = running.withTask(9)
        assertEquals(9L, s.taskId)
        assertTrue(s.running)
        assertEquals(running.endsAtMillis, s.endsAtMillis)
    }
}
