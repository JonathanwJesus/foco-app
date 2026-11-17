@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.foco.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foco.app.AppViewModel
import com.foco.app.data.DefaultDue
import com.foco.app.data.Fmt
import com.foco.app.data.Priority
import com.foco.app.data.Subject
import com.foco.app.ui.ButtonKind
import com.foco.app.ui.FocoButton
import com.foco.app.ui.FocoCard
import com.foco.app.ui.FocoChip
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.FocoTextField
import com.foco.app.ui.FocoTopBar
import com.foco.app.ui.SwitchRow
import com.foco.app.ui.rememberNotificationPermissionRequester
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun NewTaskScreen(vm: AppViewModel, onClose: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf(Subject.CALCULO) }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    val suggestion = remember { DefaultDue.suggest(LocalDateTime.now()) }
    var dayEpoch by rememberSaveable { mutableStateOf(suggestion.first.toEpochDay()) }
    var minutes by rememberSaveable { mutableStateOf(suggestion.second) }
    var reminder by rememberSaveable { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val requestNotifications = rememberNotificationPermissionRequester()

    val day = LocalDate.ofEpochDay(dayEpoch)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        FocoTopBar("Nova tarefa", onClose)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FocoTextField(
                label = "Título da tarefa",
                value = title,
                onValueChange = { title = it },
                placeholder = "Ex.: Resolver lista 5",
                leadingIcon = FocoIcon.BookOpen,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Disciplina", style = FocoText.Label14, color = FocoColor.Text)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Subject.entries.forEach { s ->
                        FocoChip(s.label, subject == s, { subject = s })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocoTextField(
                    label = "Data",
                    value = when (day) {
                        LocalDate.now() -> "Hoje"
                        LocalDate.now().plusDays(1) -> "Amanhã"
                        else -> Fmt.shortDate(day)
                    },
                    onValueChange = {},
                    leadingIcon = FocoIcon.Calendar,
                    onClick = { showDate = true },
                    modifier = Modifier.weight(1f),
                )
                FocoTextField(
                    label = "Horário",
                    value = Fmt.time(minutes),
                    onValueChange = {},
                    leadingIcon = FocoIcon.Clock,
                    onClick = { showTime = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prioridade", style = FocoText.Label14, color = FocoColor.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        FocoChip(p.label, priority == p, { priority = p })
                    }
                }
            }

            FocoCard(padding = 0.dp) {
                SwitchRow(
                    title = "Lembrete",
                    subtitle = "Avisar 30 minutos antes",
                    checked = reminder,
                    onCheckedChange = { reminder = it },
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocoButton(
                text = "Salvar tarefa",
                enabled = title.isNotBlank(),
                onClick = {
                    if (reminder) requestNotifications()
                    vm.addTask(title, subject, day, minutes, priority, reminder)
                    onClose()
                },
            )
            FocoButton("Cancelar", onClose, kind = ButtonKind.Ghost)
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dayEpoch = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    minutes = state.hour * 60 + state.minute
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
            title = { Text("Horário") },
            text = {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    TimePicker(state = state)
                }
            },
        )
    }
}
