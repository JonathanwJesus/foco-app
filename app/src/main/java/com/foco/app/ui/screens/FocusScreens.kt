package com.foco.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.app.AppViewModel
import com.foco.app.timer.TimerMode
import com.foco.app.data.Fmt
import com.foco.app.data.Stats
import com.foco.app.ui.ButtonKind
import com.foco.app.ui.CircleIconButton
import com.foco.app.ui.FIcon
import com.foco.app.ui.FocoButton
import com.foco.app.ui.FocoChip
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.ProgressRing
import com.foco.app.ui.StatCard
import com.foco.app.ui.SubjectTag
import com.foco.app.ui.rememberNotificationPermissionRequester
import java.time.LocalDate

// =========================================================================
// 05 Modo foco
// =========================================================================

@Composable
fun FocusScreen(vm: AppViewModel) {
    val timer by vm.timer.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val requestNotifications = rememberNotificationPermissionRequester()
    val task = tasks.firstOrNull { it.id == timer.taskId }
    val sessionsToday = Stats.sessionsOn(sessions, today)
    var showPicker by remember { mutableStateOf(false) }

    // mantém a tela ligada enquanto o timer roda (se a preferência estiver ativa)
    val view = LocalView.current
    DisposableEffect(timer.running, profile.keepScreenOn) {
        view.keepScreenOn = timer.running && profile.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Modo foco", style = FocoText.Title24, color = FocoColor.Text)
            Text("Uma tarefa por vez, sem distrações", style = FocoText.Body14, color = FocoColor.TextMuted)
        }

        // tarefa atual
        Surface(
            onClick = { showPicker = true },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            color = FocoColor.PrimarySoft,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FIcon(FocoIcon.BookOpen, tint = FocoColor.PrimaryDark, size = 18.dp)
                Text(
                    text = task?.let { "${it.title} · ${it.subject.label}" } ?: "Escolher uma tarefa",
                    style = FocoText.Label14,
                    color = FocoColor.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        // duração
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerMode.entries.forEach { mode ->
                FocoChip(mode.label, timer.mode == mode, { vm.selectTimerMode(mode) })
            }
        }

        // anel do timer
        ProgressRing(
            progress = timer.progress,
            size = 264.dp,
            thickness = 16.dp,
            trackColor = FocoColor.PrimarySoft,
            color = if (timer.mode == TimerMode.FOCUS) FocoColor.Primary else FocoColor.Success,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Fmt.clock(timer.remainingSec), style = FocoText.Number48, color = FocoColor.Text)
                Text(
                    if (timer.mode == TimerMode.FOCUS) "Sessão ${sessionsToday + 1} de ${profile.dailyGoal}" else "Hora de descansar",
                    style = FocoText.Caption12,
                    color = FocoColor.TextMuted,
                )
            }
        }

        // controles
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(FocoIcon.Reset, "Reiniciar", { vm.resetTimer() }, size = 56.dp, iconSize = 24.dp)
            CircleIconButton(
                icon = if (timer.running) FocoIcon.Pause else FocoIcon.Play,
                description = if (timer.running) "Pausar" else "Iniciar",
                onClick = {
                    // pede a permissão de notificações ao iniciar, para o aviso de fim funcionar
                    if (!timer.running) requestNotifications()
                    vm.toggleTimer()
                },
                size = 80.dp,
                iconSize = 32.dp,
                background = FocoColor.Primary,
                tint = Color.White,
                border = null,
            )
            CircleIconButton(FocoIcon.Close, "Encerrar", { vm.stopTimer() }, size = 56.dp, iconSize = 24.dp)
        }

        FocoButton(
            text = "Pular para o fim (demonstração)",
            onClick = { vm.finishNow() },
            kind = ButtonKind.Ghost,
        )
    }

    if (showPicker) {
        val pending = tasks.filter { !it.done }.sortedWith(compareBy({ it.dueDay }, { it.dueMinutes }))
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Em qual tarefa você vai focar?") },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    item {
                        Text(
                            "Sem tarefa (foco livre)",
                            style = FocoText.Subheading16,
                            color = FocoColor.Text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.pickTask(null); showPicker = false }
                                .padding(vertical = 12.dp),
                        )
                    }
                    items(pending, key = { it.id }) { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.pickTask(t.id); showPicker = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                t.title,
                                style = FocoText.Subheading16,
                                color = FocoColor.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            SubjectTag(t.subject)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Fechar") } },
        )
    }
}

// =========================================================================
// 06 Sessão concluída
// =========================================================================

@Composable
fun SessionDoneScreen(vm: AppViewModel, onHome: () -> Unit, onBreak: () -> Unit) {
    val last by vm.lastSession.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val minutes = last?.minutes ?: 25
    val task = tasks.firstOrNull { it.id == last?.taskId }
    val streak = Stats.streak(sessions, today)
    val minutesToday = Stats.minutesOn(sessions, today)
    val sessionsToday = Stats.sessionsOn(sessions, today)
    val breakMinutes = if (sessionsToday > 0 && sessionsToday % 4 == 0) 15 else 5

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(136.dp).clip(CircleShape).background(FocoColor.SuccessSoft),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(92.dp).clip(CircleShape).background(FocoColor.Success),
                    contentAlignment = Alignment.Center,
                ) { FIcon(FocoIcon.Check, tint = Color.White, size = 48.dp, description = "Concluído") }
            }
            Text("Sessão concluída!", style = FocoText.Title24, color = FocoColor.Text, textAlign = TextAlign.Center)
            Text(
                "Você focou por $minutes minutos" +
                    (last?.subject?.let { " em ${it.label}" } ?: "") +
                    ". Ótimo trabalho, continue assim!",
                style = FocoText.Body16,
                color = FocoColor.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Fmt.duration(minutesToday), "Hoje", FocoIcon.Clock, FocoColor.Primary, Modifier.weight(1f))
                StatCard(
                    if (streak == 1) "1 dia" else "$streak dias",
                    "Sequência",
                    FocoIcon.Flame,
                    FocoColor.Accent,
                    Modifier.weight(1f),
                )
                StatCard("$sessionsToday", "Sessões", FocoIcon.Target, FocoColor.Success, Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (task != null && !task.done) {
                FocoButton("Marcar tarefa como feita", { vm.markLastSessionTaskDone(); onHome() })
            } else {
                FocoButton("Voltar ao início", onHome)
            }
            FocoButton(
                "Iniciar pausa de $breakMinutes min",
                { vm.startBreak(); onBreak() },
                kind = ButtonKind.Secondary,
            )
        }
    }
}
