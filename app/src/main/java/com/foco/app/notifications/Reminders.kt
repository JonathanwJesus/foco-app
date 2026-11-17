package com.foco.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.data.Task
import java.time.LocalDate
import java.time.ZoneId

/** Agenda lembretes de tarefas (30 min antes) e o lembrete diário (8h) com o AlarmManager. */
object Reminders {

    const val CHANNEL_ID = "foco_lembretes"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_ID = "id"

    private const val ACTION = "com.foco.app.REMINDER"
    private const val DAILY_CODE = 1
    private const val MINUTES_BEFORE = 30

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Lembretes de tarefas e da rotina de estudos" }
        nm.createNotificationChannel(channel)
    }

    private fun requestCode(taskId: Long): Int = (taskId % 1_000_000_000L).toInt() + 10

    private fun pendingIntent(
        context: Context,
        code: Int,
        title: String,
        text: String,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_ID, code)
        return PendingIntent.getBroadcast(context, code, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Agenda o aviso de uma tarefa (se o lembrete estiver ligado e o horário ainda não passou). */
    fun scheduleTask(context: Context, task: Task) {
        if (!task.reminder || task.done) return
        val dueMillis = LocalDate.ofEpochDay(task.dueDay)
            .atTime(task.dueMinutes / 60, task.dueMinutes % 60)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val triggerAt = dueMillis - MINUTES_BEFORE * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(
            context,
            requestCode(task.id),
            task.title,
            "Começa em $MINUTES_BEFORE minutos · ${task.subject.label}",
            PendingIntent.FLAG_UPDATE_CURRENT,
        ) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        pendingIntent(context, requestCode(taskId), "", "", PendingIntent.FLAG_NO_CREATE)
            ?.let { alarm.cancel(it) }
    }

    /** Liga/desliga o lembrete diário das 8h. */
    fun setDaily(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (enabled) {
            var first = LocalDate.now().atTime(8, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (first <= System.currentTimeMillis()) first += AlarmManager.INTERVAL_DAY
            val pi = pendingIntent(
                context,
                DAILY_CODE,
                "Hora de planejar o dia",
                "Abra o Foco e organize suas tarefas de hoje.",
                PendingIntent.FLAG_UPDATE_CURRENT,
            ) ?: return
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pi)
        } else {
            pendingIntent(context, DAILY_CODE, "", "", PendingIntent.FLAG_NO_CREATE)
                ?.let { alarm.cancel(it) }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        Reminders.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_target)
            .setContentTitle(intent.getStringExtra(Reminders.EXTRA_TITLE) ?: "Foco")
            .setContentText(intent.getStringExtra(Reminders.EXTRA_TEXT) ?: "")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(intent.getIntExtra(Reminders.EXTRA_ID, 0), notification)
    }
}
