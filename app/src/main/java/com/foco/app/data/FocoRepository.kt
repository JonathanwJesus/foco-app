package com.foco.app.data

import com.foco.app.timer.TimerState

/**
 * Onde o app guarda e lê os dados. A implementação real ([FocoStore]) usa o SharedPreferences;
 * nos testes dá para usar uma versão em memória.
 */
interface FocoRepository {
    fun loadTasks(): List<Task>
    fun saveTasks(tasks: List<Task>)

    fun loadSessions(): List<FocusSession>
    fun saveSessions(sessions: List<FocusSession>)

    fun loadProfile(): Profile
    fun saveProfile(p: Profile)

    /** Estado do timer, guardado para sobreviver se o Android encerrar o app em segundo plano. */
    fun loadTimer(): TimerState
    fun saveTimer(state: TimerState)

    fun clearAll()
}
