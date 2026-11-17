package com.foco.app.data

import android.content.Context
import com.foco.app.timer.TimerMode
import com.foco.app.timer.TimerState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistência local simples: guarda tarefas, sessões e perfil em JSON dentro do
 * SharedPreferences do app. Tudo fica no aparelho (não há servidor).
 */
class FocoStore(context: Context) : FocoRepository {

    private val prefs = context.getSharedPreferences("foco_data", Context.MODE_PRIVATE)

    // ---------- tarefas ----------
    override fun loadTasks(): List<Task> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_TASKS, null) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Task(
                id = o.getLong("id"),
                title = o.getString("title"),
                subject = Subject.from(o.getString("subject")),
                dueDay = o.getLong("dueDay"),
                dueMinutes = o.getInt("dueMinutes"),
                priority = runCatching { Priority.valueOf(o.getString("priority")) }
                    .getOrDefault(Priority.MEDIUM),
                reminder = o.optBoolean("reminder", false),
                done = o.optBoolean("done", false),
                doneAt = if (o.isNull("doneAt")) null else o.getLong("doneAt"),
            )
        }
    }.getOrDefault(emptyList())

    override fun saveTasks(tasks: List<Task>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("subject", t.subject.name)
                    .put("dueDay", t.dueDay)
                    .put("dueMinutes", t.dueMinutes)
                    .put("priority", t.priority.name)
                    .put("reminder", t.reminder)
                    .put("done", t.done)
                    .put("doneAt", t.doneAt ?: JSONObject.NULL)
            )
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    // ---------- sessões de foco ----------
    override fun loadSessions(): List<FocusSession> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_SESSIONS, null) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            FocusSession(
                id = o.getLong("id"),
                subject = if (o.isNull("subject")) null else Subject.from(o.getString("subject")),
                taskId = if (o.isNull("taskId")) null else o.getLong("taskId"),
                minutes = o.getInt("minutes"),
                endedAt = o.getLong("endedAt"),
            )
        }
    }.getOrDefault(emptyList())

    override fun saveSessions(sessions: List<FocusSession>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("subject", s.subject?.name ?: JSONObject.NULL)
                    .put("taskId", s.taskId ?: JSONObject.NULL)
                    .put("minutes", s.minutes)
                    .put("endedAt", s.endedAt)
            )
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    // ---------- perfil ----------
    override fun loadProfile(): Profile = runCatching {
        val o = JSONObject(prefs.getString(KEY_PROFILE, null) ?: "{}")
        Profile(
            name = o.optString("name", ""),
            email = o.optString("email", ""),
            passwordHash = o.optString("passwordHash", ""),
            loggedIn = o.optBoolean("loggedIn", false),
            dailyReminder = o.optBoolean("dailyReminder", false),
            keepScreenOn = o.optBoolean("keepScreenOn", true),
            dailyGoal = o.optInt("dailyGoal", 4),
        )
    }.getOrDefault(Profile())

    override fun saveProfile(p: Profile) {
        val o = JSONObject()
            .put("name", p.name)
            .put("email", p.email)
            .put("passwordHash", p.passwordHash)
            .put("loggedIn", p.loggedIn)
            .put("dailyReminder", p.dailyReminder)
            .put("keepScreenOn", p.keepScreenOn)
            .put("dailyGoal", p.dailyGoal)
        prefs.edit().putString(KEY_PROFILE, o.toString()).apply()
    }

    // ---------- timer ----------
    override fun loadTimer(): TimerState = runCatching {
        val o = JSONObject(prefs.getString(KEY_TIMER, null) ?: return TimerState())
        TimerState(
            mode = runCatching { TimerMode.valueOf(o.getString("mode")) }.getOrDefault(TimerMode.FOCUS),
            taskId = if (o.isNull("taskId")) null else o.getLong("taskId"),
            remainingSec = o.getInt("remainingSec"),
            endsAtMillis = if (o.isNull("endsAt")) null else o.getLong("endsAt"),
        )
    }.getOrDefault(TimerState())

    override fun saveTimer(state: TimerState) {
        val o = JSONObject()
            .put("mode", state.mode.name)
            .put("taskId", state.taskId ?: JSONObject.NULL)
            .put("remainingSec", state.remainingSec)
            .put("endsAt", state.endsAtMillis ?: JSONObject.NULL)
        prefs.edit().putString(KEY_TIMER, o.toString()).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TASKS = "tasks"
        const val KEY_SESSIONS = "sessions"
        const val KEY_PROFILE = "profile"
        const val KEY_TIMER = "timer"
    }
}
