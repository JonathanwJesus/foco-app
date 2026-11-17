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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.foco.app.MainActivity
import com.foco.app.R
import com.foco.app.timer.TimerAlarm
import com.foco.app.timer.TimerMode

/**
 * Alarme do sistema para o fim do timer. Ele dispara mesmo com o app fechado e a tela apagada,
 * e o [TimerReceiver] mostra a notificação.
 *
 * Quem registra a sessão no histórico é sempre o [com.foco.app.AppViewModel] (ao terminar com o app
 * aberto, ou ao reabrir o app); o alarme só avisa o usuário.
 */
class AndroidTimerAlarm(private val context: Context) : TimerAlarm {

    override fun schedule(endsAtMillis: Long, mode: TimerMode) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(mode, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        // A partir do Android 12 o alarme exato depende de permissão; sem ela, usa o alarme normal.
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pi)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pi)
        }
    }

    override fun cancel() {
        val alarm = context.getSystemService(AlarmManager::class.java)
        pendingIntent(TimerMode.FOCUS, PendingIntent.FLAG_NO_CREATE)?.let {
            alarm.cancel(it)
            it.cancel()
        }
        TimerNotifications.cancel(context)
    }

    private fun pendingIntent(mode: TimerMode, flags: Int): PendingIntent? {
        val intent = Intent(context, TimerReceiver::class.java)
            .setAction(TimerNotifications.ACTION_TIMER_END)
            .putExtra(TimerNotifications.EXTRA_MODE, mode.name)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val REQUEST_CODE = 2
    }
}

object TimerNotifications {
    const val CHANNEL_ID = "foco_timer"
    const val NOTIFICATION_ID = 4242
    const val ACTION_TIMER_END = "com.foco.app.TIMER_END"
    const val EXTRA_MODE = "mode"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer de foco",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Avisa quando uma sessão de foco ou uma pausa termina" }
        nm.createNotificationChannel(channel)
    }

    fun showFinished(context: Context, mode: TimerMode) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = if (mode == TimerMode.FOCUS) {
            "Sessão de foco concluída" to "Você focou ${mode.minutes} minutos. Que tal uma pausa?"
        } else {
            "Pausa terminou" to "Hora de voltar ao foco."
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Com o app na tela, a própria interface mostra que o timer terminou.
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val mode = runCatching {
            TimerMode.valueOf(intent.getStringExtra(TimerNotifications.EXTRA_MODE) ?: "")
        }.getOrDefault(TimerMode.FOCUS)
        TimerNotifications.showFinished(context, mode)
    }
}
