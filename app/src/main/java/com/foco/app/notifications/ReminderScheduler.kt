package com.foco.app.notifications

import android.content.Context
import com.foco.app.data.Task

/** Agenda e cancela lembretes. Separado do ViewModel para ele poder ser testado sem o Android. */
interface ReminderScheduler {
    fun scheduleTask(task: Task)
    fun cancelTask(taskId: Long)
    fun setDaily(enabled: Boolean)
}

/** Implementação real, com o AlarmManager (veja [Reminders]). */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun scheduleTask(task: Task) = Reminders.scheduleTask(context, task)
    override fun cancelTask(taskId: Long) = Reminders.cancelTask(context, taskId)
    override fun setDaily(enabled: Boolean) = Reminders.setDaily(context, enabled)
}
