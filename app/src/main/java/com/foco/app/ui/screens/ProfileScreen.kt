@file:OptIn(ExperimentalLayoutApi::class)

package com.foco.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.app.AppViewModel
import com.foco.app.data.Fmt
import com.foco.app.data.Stats
import com.foco.app.ui.ButtonKind
import com.foco.app.ui.FIcon
import com.foco.app.ui.FocoButton
import com.foco.app.ui.FocoCard
import com.foco.app.ui.FocoChip
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.StatCard
import com.foco.app.ui.SwitchRow
import com.foco.app.ui.rememberNotificationPermissionRequester
import java.time.LocalDate

@Composable
fun ProfileScreen(vm: AppViewModel, onLoggedOut: () -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val requestNotifications = rememberNotificationPermissionRequester()

    var showGoal by remember { mutableStateOf(false) }
    var showErase by remember { mutableStateOf(false) }

    val streak = Stats.streak(sessions, today)
    val tasksDone = tasks.count { it.done }
    val totalMinutes = sessions.sumOf { it.minutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Perfil", style = FocoText.Title24, color = FocoColor.Text)

        FocoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(FocoColor.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        profile.firstName.take(1).uppercase(),
                        style = FocoText.Heading20,
                        color = FocoColor.PrimaryDark,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(profile.name.ifBlank { "Estudante" }, style = FocoText.Heading20, color = FocoColor.Text)
                    Text(profile.email, style = FocoText.Body14, color = FocoColor.TextSecondary)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("$streak", "Sequência", FocoIcon.Flame, FocoColor.Accent, Modifier.weight(1f))
            StatCard("$tasksDone", "Tarefas", FocoIcon.Check, FocoColor.Success, Modifier.weight(1f))
            StatCard(
                if (totalMinutes < 60) "${totalMinutes}min" else "${totalMinutes / 60}h",
                "Foco total",
                FocoIcon.Timer,
                FocoColor.Primary,
                Modifier.weight(1f),
            )
        }

        Text("Preferências", style = FocoText.Heading20, color = FocoColor.Text)
        FocoCard(padding = 0.dp) {
            SwitchRow(
                title = "Lembretes diários",
                subtitle = "Aviso às 8h para planejar o dia",
                checked = profile.dailyReminder,
                onCheckedChange = {
                    if (it) requestNotifications()
                    vm.setDailyReminder(it)
                },
            )
            HorizontalDivider(color = FocoColor.Border)
            SwitchRow(
                title = "Manter a tela ligada",
                subtitle = "Evita que a tela apague durante o foco",
                checked = profile.keepScreenOn,
                onCheckedChange = { vm.setKeepScreenOn(it) },
            )
            HorizontalDivider(color = FocoColor.Border)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showGoal = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Meta diária", style = FocoText.Subheading16, color = FocoColor.Text)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${profile.dailyGoal} ${if (profile.dailyGoal == 1) "sessão" else "sessões"}",
                        style = FocoText.Body14,
                        color = FocoColor.TextSecondary,
                    )
                    FIcon(FocoIcon.ChevronRight, tint = FocoColor.TextMuted, size = 20.dp)
                }
            }
        }

        Text("Dados", style = FocoText.Heading20, color = FocoColor.Text)
        FocoCard(padding = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Para apresentar o app, carregue tarefas e sessões de exemplo. Você pode apagar tudo depois.",
                    style = FocoText.Body14,
                    color = FocoColor.TextSecondary,
                )
                FocoButton("Gerar dados de exemplo", { vm.loadSamples() }, kind = ButtonKind.Secondary)
                FocoButton("Apagar todos os dados", { showErase = true }, kind = ButtonKind.Ghost)
            }
        }

        FocoButton("Sair da conta", { vm.logout(); onLoggedOut() }, kind = ButtonKind.Secondary)
        Text(
            "Foco 1.0 · Protótipo funcional · ${Fmt.shortDate(today)}",
            style = FocoText.Caption12,
            color = FocoColor.TextMuted,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    if (showGoal) {
        AlertDialog(
            onDismissRequest = { showGoal = false },
            title = { Text("Meta diária de sessões") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..8).forEach { n ->
                        FocoChip("$n", profile.dailyGoal == n, { vm.setDailyGoal(n); showGoal = false })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGoal = false }) { Text("Fechar") } },
        )
    }

    if (showErase) {
        AlertDialog(
            onDismissRequest = { showErase = false },
            title = { Text("Apagar todos os dados?") },
            text = { Text("Isso remove sua conta, tarefas e sessões deste aparelho. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { showErase = false; vm.eraseAll(); onLoggedOut() }) {
                    Text("Apagar", color = FocoColor.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { showErase = false }) { Text("Cancelar") } },
        )
    }
}
