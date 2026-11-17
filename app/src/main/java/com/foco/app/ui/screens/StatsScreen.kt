package com.foco.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.app.AppViewModel
import com.foco.app.data.Fmt
import com.foco.app.data.Stats
import com.foco.app.data.Subject
import com.foco.app.ui.BarChart
import com.foco.app.ui.FIcon
import com.foco.app.ui.FocoCard
import com.foco.app.ui.FocoChip
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.ProgressRow
import com.foco.app.ui.fg
import java.time.LocalDate

@Composable
fun StatsScreen(vm: AppViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var period by rememberSaveable { mutableStateOf(Stats.Period.WEEK) }

    val data = remember(sessions, period) { Stats.compute(period, sessions, today) }
    val bySubjectTotal = data.bySubject.sumOf { it.second }.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Estatísticas", style = FocoText.Title24, color = FocoColor.Text)
                Text("Acompanhe sua evolução", style = FocoText.Body14, color = FocoColor.TextMuted)
            }
        }

        item {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stats.Period.entries.forEach { p ->
                    FocoChip(p.label, period == p, { period = p })
                }
            }
        }

        // resumo
        item {
            FocoCard(Modifier.padding(horizontal = 24.dp)) {
                Text(period.totalTitle, style = FocoText.Caption12, color = FocoColor.TextMuted)
                Text(
                    Fmt.duration(data.total),
                    style = FocoText.Display32,
                    color = FocoColor.Text,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
                val delta = data.deltaPercent
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (delta == null || delta >= 0) FocoColor.SuccessSoft else FocoColor.DangerSoft)
                        .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (delta != null) {
                        FIcon(
                            FocoIcon.TrendingUp,
                            modifier = Modifier.rotate(if (delta >= 0) 0f else 90f),
                            tint = if (delta >= 0) FocoColor.SuccessStrong else FocoColor.DangerStrong,
                            size = 14.dp,
                        )
                    }
                    Text(
                        text = when {
                            delta == null -> "Sem período anterior para comparar"
                            delta >= 0 -> "+$delta% vs. período anterior"
                            else -> "$delta% vs. período anterior"
                        },
                        style = FocoText.Caption12,
                        color = if (delta == null || delta >= 0) FocoColor.SuccessStrong else FocoColor.DangerStrong,
                    )
                }
            }
        }

        // gráfico
        item {
            FocoCard(Modifier.padding(horizontal = 24.dp)) {
                Text("Minutos por ${when (period) {
                    Stats.Period.WEEK -> "dia"
                    Stats.Period.MONTH -> "semana"
                    Stats.Period.SEMESTER -> "mês"
                }}", style = FocoText.Subheading16, color = FocoColor.Text)
                BarChart(data.bars, Modifier.padding(top = 16.dp))
            }
        }

        // por disciplina
        item {
            FocoCard(Modifier.padding(horizontal = 24.dp)) {
                Text("Por disciplina", style = FocoText.Subheading16, color = FocoColor.Text)
                if (data.bySubject.isEmpty()) {
                    Text(
                        "Conclua sessões de foco com uma tarefa escolhida para ver a divisão por disciplina.",
                        style = FocoText.Body14,
                        color = FocoColor.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        data.bySubject.forEach { (subject: Subject, minutes: Int) ->
                            val pct = minutes * 100 / bySubjectTotal
                            ProgressRow(
                                label = subject.label,
                                valueText = "$pct%  ·  ${Fmt.duration(minutes)}",
                                fraction = minutes.toFloat() / bySubjectTotal,
                                color = subject.fg,
                            )
                        }
                    }
                }
            }
        }
    }
}
