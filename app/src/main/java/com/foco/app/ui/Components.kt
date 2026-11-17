package com.foco.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.Fmt
import com.foco.app.data.Priority
import com.foco.app.data.Stats
import com.foco.app.data.Subject
import com.foco.app.data.Task
import java.time.LocalDate

/** Ícones do app (arquivos res/drawable/ic_*.xml, do conjunto Lucide, usados também no Figma). */
object FocoIcon {
    val Home = R.drawable.ic_home
    val Plus = R.drawable.ic_plus
    val Check = R.drawable.ic_check
    val ChevronRight = R.drawable.ic_chevron_right
    val ChevronLeft = R.drawable.ic_chevron_left
    val BarChart = R.drawable.ic_bar_chart
    val Timer = R.drawable.ic_timer
    val Calendar = R.drawable.ic_calendar
    val Mail = R.drawable.ic_mail
    val Lock = R.drawable.ic_lock
    val User = R.drawable.ic_user
    val Eye = R.drawable.ic_eye
    val Flame = R.drawable.ic_flame
    val Play = R.drawable.ic_play
    val Pause = R.drawable.ic_pause
    val Close = R.drawable.ic_x
    val Bell = R.drawable.ic_bell
    val ArrowRight = R.drawable.ic_arrow_right
    val TrendingUp = R.drawable.ic_trending_up
    val Target = R.drawable.ic_target
    val Flag = R.drawable.ic_flag
    val BookOpen = R.drawable.ic_book_open
    val Clock = R.drawable.ic_clock
    val Reset = R.drawable.ic_rotate_ccw
}

@Composable
fun FIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = FocoColor.Text,
    size: Dp = 24.dp,
    description: String? = null,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = description,
        modifier = modifier.size(size),
        tint = tint,
    )
}

// =========================================================================
// Botões
// =========================================================================

enum class ButtonKind { Primary, Secondary, Ghost }

@Composable
fun FocoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
) {
    val bg = when (kind) {
        ButtonKind.Primary -> if (enabled) FocoColor.Primary else FocoColor.Border
        ButtonKind.Secondary -> FocoColor.Surface
        ButtonKind.Ghost -> Color.Transparent
    }
    val fg = when (kind) {
        ButtonKind.Primary -> if (enabled) Color.White else FocoColor.TextMuted
        else -> if (enabled) FocoColor.Primary else FocoColor.TextMuted
    }
    val border = if (kind == ButtonKind.Secondary) {
        BorderStroke(1.5.dp, if (enabled) FocoColor.Primary else FocoColor.Border)
    } else null

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
            Text(text, style = FocoText.Button16, color = fg)
        }
    }
}

@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    background: Color = FocoColor.Surface,
    tint: Color = FocoColor.Text,
    border: Color? = FocoColor.Border,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        border = border?.let { BorderStroke(1.dp, it) },
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            FIcon(icon, tint = tint, size = iconSize, description = description)
        }
    }
}

@Composable
fun FocoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) FocoColor.PrimarySoft else FocoColor.Surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) FocoColor.Primary else FocoColor.Border,
        ),
        modifier = modifier,
    ) {
        Text(
            label,
            style = FocoText.Label14,
            color = if (selected) FocoColor.PrimaryDark else FocoColor.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

// =========================================================================
// Campos de formulário
// =========================================================================

@Composable
fun FocoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes leadingIcon: Int? = null,
    error: String? = null,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** Se informado, o campo é só leitura e o toque chama [onClick] (ex.: abrir seletor de data). */
    onClick: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FocoText.Label14, color = FocoColor.Text)
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = onClick != null,
                isError = error != null,
                shape = RoundedCornerShape(12.dp),
                textStyle = FocoText.Body16,
                placeholder = { Text(placeholder, style = FocoText.Body16, color = FocoColor.TextMuted) },
                leadingIcon = leadingIcon?.let {
                    { FIcon(it, tint = FocoColor.TextMuted, size = 22.dp) }
                },
                trailingIcon = if (password) {
                    {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .clickable { visible = !visible }
                                .padding(8.dp)
                        ) {
                            FIcon(
                                FocoIcon.Eye,
                                tint = if (visible) FocoColor.Primary else FocoColor.TextMuted,
                                size = 22.dp,
                                description = if (visible) "Ocultar senha" else "Mostrar senha",
                            )
                        }
                    }
                } else null,
                visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocoColor.Primary,
                    unfocusedBorderColor = FocoColor.Border,
                    errorBorderColor = FocoColor.Danger,
                    focusedContainerColor = FocoColor.Surface,
                    unfocusedContainerColor = FocoColor.Surface,
                    errorContainerColor = FocoColor.Surface,
                    focusedTextColor = FocoColor.Text,
                    unfocusedTextColor = FocoColor.Text,
                    errorTextColor = FocoColor.Text,
                    cursorColor = FocoColor.Primary,
                ),
            )
            if (onClick != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                )
            }
        }
        if (error != null) {
            Text(error, style = FocoText.Caption12, color = FocoColor.Danger)
        }
    }
}

// =========================================================================
// Cartões e listas
// =========================================================================

@Composable
fun FocoCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FocoColor.Surface,
        border = BorderStroke(1.dp, FocoColor.Border),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun SubjectTag(subject: Subject) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(subject.bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(subject.label, style = FocoText.Caption12, color = subject.fg)
    }
}

@Composable
private fun CheckCircle(done: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .semantics { contentDescription = if (done) "Desmarcar tarefa" else "Concluir tarefa" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) FocoColor.Success else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                FIcon(FocoIcon.Check, tint = Color.White, size = 16.dp)
            } else {
                Canvas(Modifier.size(28.dp)) {
                    drawCircle(
                        color = FocoColor.TextMuted,
                        radius = size.minDimension / 2 - 1.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    today: LocalDate,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FocoColor.Surface,
        border = BorderStroke(1.dp, FocoColor.Border),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckCircle(done = task.done, onToggle = onToggle)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title,
                    style = FocoText.Subheading16,
                    color = if (task.done) FocoColor.TextMuted else FocoColor.Text,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SubjectTag(task.subject)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FIcon(FocoIcon.Clock, tint = FocoColor.TextMuted, size = 14.dp)
                        Text(Fmt.dueText(task, today), style = FocoText.Caption12, color = FocoColor.TextMuted)
                    }
                    if (task.priority == Priority.HIGH && !task.done) {
                        FIcon(FocoIcon.Flag, tint = FocoColor.Danger, size = 14.dp, description = "Prioridade alta")
                    }
                }
            }
            FIcon(FocoIcon.ChevronRight, tint = FocoColor.TextMuted, size = 20.dp)
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FocoColor.Surface,
        border = BorderStroke(1.dp, FocoColor.Border),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FIcon(icon, tint = tint, size = 22.dp)
            Text(value, style = FocoText.Heading20, color = FocoColor.Text)
            Text(label, style = FocoText.Caption12, color = FocoColor.TextMuted)
        }
    }
}

// =========================================================================
// Gráficos
// =========================================================================

/** Anel de progresso (usado na meta do dia e no timer). */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    thickness: Dp,
    trackColor: Color,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    // pontas redondas só em arcos parciais; perto do círculo cheio elas se sobrepõem e criam um "dente"
                    style = Stroke(
                        width = stroke,
                        cap = if (progress >= 0.97f) StrokeCap.Butt else StrokeCap.Round,
                    ),
                )
            }
        }
        content()
    }
}

/** Gráfico de barras simples (minutos por dia/semana/mês). */
@Composable
fun BarChart(bars: List<Stats.Bar>, modifier: Modifier = Modifier, chartHeight: Dp = 130.dp) {
    val max = (bars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (bar.minutes > 0) bar.minutes.toString() else "",
                    style = FocoText.Caption12,
                    color = if (bar.highlight) FocoColor.PrimaryDark else FocoColor.TextMuted,
                    modifier = Modifier.height(16.dp),
                )
                val h = if (bar.minutes == 0) 4.dp else (chartHeight * bar.minutes / max).coerceAtLeast(6.dp)
                Box(
                    Modifier
                        .width(28.dp)
                        .height(h)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (bar.highlight) FocoColor.Primary else FocoColor.PrimarySoft)
                )
                Text(
                    bar.label,
                    style = FocoText.Caption12,
                    color = if (bar.highlight) FocoColor.Text else FocoColor.TextMuted,
                )
            }
        }
    }
}

/** Linha de rótulo + valor com barra de progresso (estatísticas por disciplina). */
@Composable
fun ProgressRow(label: String, valueText: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = FocoText.Label14, color = FocoColor.Text)
            Text(valueText, style = FocoText.Label14, color = FocoColor.TextSecondary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(FocoColor.Background)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

// =========================================================================
// Barras de navegação
// =========================================================================

@Composable
fun FocoTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleIconButton(FocoIcon.ChevronLeft, "Voltar", onBack)
        Text(title, style = FocoText.Heading20, color = FocoColor.Text)
    }
}

enum class Tab(val route: String, val label: String, @DrawableRes val icon: Int) {
    Home("home", "Hoje", FocoIcon.Home),
    Stats("stats", "Estatísticas", FocoIcon.BarChart),
    Focus("focus", "Foco", FocoIcon.Timer),
    Profile("profile", "Perfil", FocoIcon.User),
}

@Composable
private fun RowScopeTab(tab: Tab, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FIcon(tab.icon, tint = if (active) FocoColor.Primary else FocoColor.TextMuted)
        Text(
            tab.label,
            style = FocoText.Caption12,
            color = if (active) FocoColor.Primary else FocoColor.TextMuted,
        )
    }
}

/** Barra de abas com o botão "+" em destaque no centro. */
@Composable
fun FocoBottomBar(currentRoute: String?, onTab: (Tab) -> Unit, onAdd: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            color = FocoColor.Surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
        ) {
            Column {
                HorizontalDivider(color = FocoColor.Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RowScopeTab(Tab.Home, currentRoute == Tab.Home.route, { onTab(Tab.Home) }, Modifier.weight(1f))
                    RowScopeTab(Tab.Stats, currentRoute == Tab.Stats.route, { onTab(Tab.Stats) }, Modifier.weight(1f))
                    Spacer(Modifier.width(64.dp))
                    RowScopeTab(Tab.Focus, currentRoute == Tab.Focus.route, { onTab(Tab.Focus) }, Modifier.weight(1f))
                    RowScopeTab(Tab.Profile, currentRoute == Tab.Profile.route, { onTab(Tab.Profile) }, Modifier.weight(1f))
                }
            }
        }
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = FocoColor.Primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                FIcon(FocoIcon.Plus, tint = Color.White, size = 28.dp, description = "Nova tarefa")
            }
        }
    }
}

// =========================================================================
// Interruptor com texto
// =========================================================================

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = FocoText.Subheading16, color = FocoColor.Text)
            Text(subtitle, style = FocoText.Caption12, color = FocoColor.TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FocoColor.Primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = FocoColor.Border,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
