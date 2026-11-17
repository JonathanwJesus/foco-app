package com.foco.app

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.foco.app.data.FocoStore
import com.foco.app.notifications.AlarmReminderScheduler
import com.foco.app.notifications.AndroidTimerAlarm

/** Monta o [AppViewModel] com as peças reais do Android (armazenamento e alarmes). */
fun appViewModelFactory(context: Context): ViewModelProvider.Factory {
    val appContext = context.applicationContext
    return viewModelFactory {
        initializer {
            AppViewModel(
                repo = FocoStore(appContext),
                reminders = AlarmReminderScheduler(appContext),
                timerAlarm = AndroidTimerAlarm(appContext),
            )
        }
    }
}
