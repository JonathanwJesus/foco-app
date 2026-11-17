package com.foco.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.app.AppViewModel
import com.foco.app.data.Fmt
import com.foco.app.data.Stats
import com.foco.app.data.Subject
import com.foco.app.data.Task
import com.foco.app.ui.ButtonKind
import com.foco.app.ui.CircleIconButton
import com.foco.app.ui.FIcon
import com.foco.app.ui.FocoButton
import com.foco.app.ui.FocoCard
import com.foco.app.ui.FocoChip
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.ProgressRing
import com.foco.app.ui.TaskCard
import java.time.LocalDate

@Composable
fun HomeScreen(vm: AppViewModel, onOpenTask: (Long) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    var filter by remember { mutableStateOf<Subject?>(null) }
    var toDelete by remember { mutableStateOf<Task?>(null) }
    var showBell by remember { mutableStateOf(false) }

    val pending = tasks
        .filter { !it.done }
        .filter { filter == null || it.subject == filter }
        .sortedWith(compareBy<Task>({ it.dueDay }, { it.dueMinutes }))
    val doneToday = tasks.filter { t -> t.done && t.doneAt?.let { Stats.dayOf(it) == today } == true }
    val pendingToday = tasks.count { !it.done && it.dueDay == today.toEpochDay() }
    val totalToday = pendingToday + doneToday.size
    val progress = if (totalToday == 0) 0f else doneToday.size.toFloat() / totalToday
    val streak = Stats.streak(sessions, today)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ----- saudação -----
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(Fmt.longDate(today), style = FocoText.Body14, color = FocoColor.TextMuted)
                    Text("Olá, ${profile.firstName}", style = FocoText.Title24, color = FocoColor.Text)
                }
                Box {
                    CircleIconButton(FocoIcon.Bell, "Notificações", { showBell = true })
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 10.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(FocoColor.Danger)
                    )
                }
            }
        }

        // ----- meta do dia -----
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FocoColor.Primary,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp).fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 24.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Meta de hoje", style = FocoText.Caption12, color = Color.White.copy(alpha = 0.85f))
                        Text(
                            "${doneToday.size} de $totalToday ${if (totalToday == 1) "tarefa" else "tarefas"}",
                            style = FocoText.Heading20,
                            color = Color.White,
                        )
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(FocoColor.PrimaryDark)
                                .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FIcon(FocoIcon.Flame, tint = FocoColor.Accent, size = 16.dp)
                            Text(
                                if (streak == 1) "1 dia seguido" else "$streak dias seguidos",
                                style = FocoText.Caption12,
                                color = Color.White,
                            )
                        }
                    }
                    ProgressRing(
                        progress = progress,
                        size = 92.dp,
                        thickness = 10.dp,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        color = Color.White,
                    ) {
                        Text("${(progress * 100).toInt()}%", style = FocoText.Subheading16, color = Color.White)
                    }
                }
            }
        }

        // ----- lista -----
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Próximas tarefas", style = FocoText.Heading20, color = FocoColor.Text)
                Text(
                    "${pending.size} pendentes",
                    style = FocoText.Label14,
                    color = FocoColor.Primary,
                )
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                item { FocoChip("Todas", filter == null, { filter = null }) }
                items(Subject.entries) { s ->
                    FocoChip(s.label, filter == s, { filter = if (filter == s) null else s })
                }
            }
        }

        if (pending.isEmpty()) {
            item {
                FocoCard(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FIcon(FocoIcon.Check, tint = FocoColor.Success, size = 32.dp)
                        Text(
                            if (tasks.isEmpty()) "Nenhuma tarefa ainda" else "Tudo em dia!",
                            style = FocoText.Subheading16,
                            color = FocoColor.Text,
                        )
                        Text(
                            if (tasks.isEmpty()) "Toque no botão + para adicionar sua primeira tarefa, ou carregue exemplos para conhecer o app."
                            else "Não há tarefas pendentes${if (filter != null) " nesta disciplina" else ""}. Que tal adicionar outra?",
                            style = FocoText.Body14,
                            color = FocoColor.TextSecondary,
                        )
                        if (tasks.isEmpty()) {
                            FocoButton("Carregar tarefas de exemplo", { vm.loadSamples() }, kind = ButtonKind.Secondary)
                        }
                    }
                }
            }
        } else {
            items(pending, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    today = today,
                    onClick = { onOpenTask(task.id) },
                    onToggle = { vm.toggleTask(task.id) },
                    onLongClick = { toDelete = task },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
            }
        }

        if (doneToday.isNotEmpty()) {
            item {
                Text(
                    "Concluídas hoje · ${doneToday.size}",
                    style = FocoText.Label14,
                    color = FocoColor.TextSecondary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
                )
            }
            items(doneToday, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    today = today,
                    onClick = { onOpenTask(task.id) },
                    onToggle = { vm.toggleTask(task.id) },
                    onLongClick = { toDelete = task },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
            }
        }
    }

    toDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Excluir tarefa?") },
            text = { Text("\"${task.title}\" será removida. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTask(task.id); toDelete = null }) {
                    Text("Excluir", color = FocoColor.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }

    if (showBell) {
        AlertDialog(
            onDismissRequest = { showBell = false },
            title = { Text("Notificações") },
            text = {
                Text("Você não tem notificações novas. Ative os lembretes no Perfil ou ao criar uma tarefa para ser avisado a tempo.")
            },
            confirmButton = { TextButton(onClick = { showBell = false }) { Text("OK") } },
        )
    }
}
