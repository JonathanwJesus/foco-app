package com.foco.app.timer

/**
 * Avisa o fim do timer mesmo com o app fechado (no Android, é um alarme do sistema).
 * Fica numa interface para o ViewModel poder ser testado sem o Android.
 */
interface TimerAlarm {
    /** Agenda o aviso para o instante em que o timer termina. Substitui o aviso anterior. */
    fun schedule(endsAtMillis: Long, mode: TimerMode)

    /** Cancela o aviso agendado e remove a notificação de fim, se estiver na tela. */
    fun cancel()
}
