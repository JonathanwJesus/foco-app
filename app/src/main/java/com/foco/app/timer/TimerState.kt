package com.foco.app.timer

enum class TimerMode(val minutes: Int, val label: String) {
    FOCUS(25, "Foco 25"),
    SHORT(5, "Pausa 5"),
    LONG(15, "Pausa 15"),
}

/** Fonte da hora atual. Existe para os testes poderem controlar o relógio. */
interface TimeSource {
    fun nowMillis(): Long
}

object SystemTimeSource : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/**
 * Estado do timer do modo foco, sem nenhuma dependência do Android.
 *
 * O timer guarda **o instante em que termina** ([endsAtMillis]) em vez de contar segundo a segundo.
 * Assim o tempo restante pode ser recalculado a qualquer momento (ao reabrir o app, por exemplo)
 * e dá para agendar um alarme para esse instante.
 *
 * Todas as operações recebem o "agora" como parâmetro e devolvem um novo estado.
 */
data class TimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val taskId: Long? = null,
    /** Segundos que faltam. Com o timer rodando, é o valor calculado no último [tick]. */
    val remainingSec: Int = mode.minutes * 60,
    /** Instante (em milissegundos) em que o timer termina; nulo se estiver parado ou pausado. */
    val endsAtMillis: Long? = null,
) {
    val running: Boolean get() = endsAtMillis != null
    val totalSec: Int get() = mode.minutes * 60
    val progress: Float get() = remainingSec.toFloat() / totalSec

    /** Segundos restantes em [nowMillis], arredondando para cima (mostra 25:00 até passar o 1º segundo). */
    fun remainingAt(nowMillis: Long): Int {
        val end = endsAtMillis ?: return remainingSec
        val ms = (end - nowMillis).coerceAtLeast(0L)
        return ((ms + 999L) / 1000L).toInt()
    }

    fun finishedAt(nowMillis: Long): Boolean {
        val end = endsAtMillis ?: return false
        return nowMillis >= end
    }

    fun start(nowMillis: Long): TimerState =
        if (running || remainingSec <= 0) this
        else copy(endsAtMillis = nowMillis + remainingSec * 1000L)

    fun pause(nowMillis: Long): TimerState =
        if (!running) this
        else copy(remainingSec = remainingAt(nowMillis), endsAtMillis = null)

    /** Atualiza [remainingSec] a partir do relógio (não faz nada se o timer não estiver rodando). */
    fun tick(nowMillis: Long): TimerState =
        if (!running) this else copy(remainingSec = remainingAt(nowMillis))

    /** Volta ao tempo cheio do modo atual, parado. */
    fun reset(): TimerState = TimerState(mode = mode, taskId = taskId)

    /** Troca o modo (foco ou pausa), parado e com o tempo cheio. */
    fun withMode(newMode: TimerMode): TimerState = TimerState(mode = newMode, taskId = taskId)

    fun withTask(id: Long?): TimerState = copy(taskId = id)

    /** Depois de concluir ou encerrar: volta ao modo foco, parado. */
    fun backToFocus(): TimerState = TimerState(mode = TimerMode.FOCUS, taskId = taskId)
}
