package com.foco.app.ui.screens

import android.util.Patterns
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foco.app.AppViewModel
import com.foco.app.ui.ButtonKind
import com.foco.app.ui.FIcon
import com.foco.app.ui.FocoButton
import com.foco.app.ui.FocoColor
import com.foco.app.ui.FocoIcon
import com.foco.app.ui.FocoText
import com.foco.app.ui.FocoTextField
import com.foco.app.ui.FocoTopBar

// =========================================================================
// 01 Onboarding
// =========================================================================

@Composable
fun OnboardingScreen(onCreateAccount: () -> Unit, onLogin: () -> Unit) {
    // a parte de cima é azul: usa ícones claros na barra de status só nesta tela
    val activity = LocalActivity.current
    val view = LocalView.current
    DisposableEffect(activity) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    Column(Modifier.fillMaxSize().background(FocoColor.Surface)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(FocoColor.Primary)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = FocoColor.PrimaryDark.copy(alpha = 0.5f),
                    radius = 160.dp.toPx(),
                    center = Offset(size.width * 0.1f, 0f),
                )
                drawCircle(
                    color = FocoColor.PrimaryDark.copy(alpha = 0.4f),
                    radius = 120.dp.toPx(),
                    center = Offset(size.width * 0.95f, size.height * 0.9f),
                )
            }

            Row(
                Modifier.statusBarsPadding().padding(start = 24.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { FIcon(FocoIcon.Target, tint = FocoColor.Primary) }
                Text("Foco", style = FocoText.Heading20, color = Color.White)
            }

            // cartão de progresso (ilustração)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FocoColor.Surface,
                shadowElevation = 12.dp,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hoje", style = FocoText.Caption12, color = FocoColor.TextMuted)
                    Text("2 de 6 tarefas", style = FocoText.Heading20, color = FocoColor.Text)
                    Box(
                        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(FocoColor.PrimarySoft)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.33f).height(10.dp).clip(RoundedCornerShape(50)).background(FocoColor.Primary)
                        )
                    }
                    Text("Falta pouco para bater sua meta", style = FocoText.Caption12, color = FocoColor.TextSecondary)
                }
            }

            // pílula da sequência
            Surface(
                shape = RoundedCornerShape(50),
                color = FocoColor.AccentSoft,
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.Center).offset(x = 105.dp, y = (-100).dp).rotate(-6f),
            ) {
                Row(
                    Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FIcon(FocoIcon.Flame, tint = FocoColor.Accent, size = 20.dp)
                    Text("7 dias", style = FocoText.Label14, color = FocoColor.Text)
                }
            }

            // pílula do timer
            Surface(
                shape = RoundedCornerShape(50),
                color = FocoColor.Surface,
                shadowElevation = 10.dp,
                modifier = Modifier.align(Alignment.Center).offset(x = (-70).dp, y = 105.dp).rotate(4f),
            ) {
                Row(
                    Modifier.padding(start = 10.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(FocoColor.Primary),
                        contentAlignment = Alignment.Center,
                    ) { FIcon(FocoIcon.Play, tint = Color.White, size = 18.dp) }
                    Text("25:00", style = FocoText.Subheading16, color = FocoColor.Text)
                }
            }
        }

        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Estude com foco, sem estresse", style = FocoText.Display32, color = FocoColor.Text)
            Text(
                "Planeje suas tarefas, use o modo foco e acompanhe sua evolução semana a semana.",
                style = FocoText.Body16,
                color = FocoColor.TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(24.dp).height(8.dp).clip(RoundedCornerShape(50)).background(FocoColor.Primary))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(FocoColor.Border))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(FocoColor.Border))
            }
            Spacer(Modifier.height(4.dp))
            FocoButton("Criar minha conta", onCreateAccount)
            FocoButton("Já tenho conta", onLogin, kind = ButtonKind.Ghost)
        }
    }
}

// =========================================================================
// 02 Cadastro
// =========================================================================

@Composable
fun SignUpScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit, onGoLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = if (name.trim().length < 2) "Informe seu nome" else null
    val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Digite um e-mail válido" else null
    val passError = if (password.length < 8) "A senha precisa ter pelo menos 8 caracteres" else null

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        FocoTopBar("Criar conta", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Crie sua conta para organizar tarefas e acompanhar seu progresso. Os dados ficam salvos neste aparelho.",
                style = FocoText.Body14,
                color = FocoColor.TextSecondary,
            )
            FocoTextField(
                label = "Nome completo",
                value = name,
                onValueChange = { name = it },
                placeholder = "Seu nome",
                leadingIcon = FocoIcon.User,
                error = if (submitted) nameError else null,
            )
            FocoTextField(
                label = "E-mail",
                value = email,
                onValueChange = { email = it },
                placeholder = "voce@email.com",
                leadingIcon = FocoIcon.Mail,
                keyboardType = KeyboardType.Email,
                error = if (submitted) emailError else null,
            )
            FocoTextField(
                label = "Senha",
                value = password,
                onValueChange = { password = it },
                placeholder = "Mínimo de 8 caracteres",
                leadingIcon = FocoIcon.Lock,
                password = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                error = if (submitted) passError else null,
            )
        }
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FocoButton("Criar conta", {
                submitted = true
                if (nameError == null && emailError == null && passError == null) {
                    vm.signUp(name, email, password)
                    onDone()
                }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Já tem uma conta?", style = FocoText.Body14, color = FocoColor.TextSecondary)
                Text(
                    "Entrar",
                    style = FocoText.Label14,
                    color = FocoColor.Primary,
                    modifier = Modifier.clickable(onClick = onGoLogin),
                )
            }
        }
    }
}

// =========================================================================
// Login (conta criada neste aparelho)
// =========================================================================

@Composable
fun LoginScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit, onGoSignUp: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        FocoTopBar("Entrar", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                if (vm.hasAccount()) "Entre com o e-mail e a senha da conta criada neste aparelho."
                else "Ainda não há conta neste aparelho. Crie uma conta para começar.",
                style = FocoText.Body14,
                color = FocoColor.TextSecondary,
            )
            FocoTextField(
                label = "E-mail",
                value = email,
                onValueChange = { email = it; error = null },
                placeholder = "voce@email.com",
                leadingIcon = FocoIcon.Mail,
                keyboardType = KeyboardType.Email,
            )
            FocoTextField(
                label = "Senha",
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Sua senha",
                leadingIcon = FocoIcon.Lock,
                password = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                error = error,
            )
        }
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FocoButton("Entrar", {
                when {
                    !vm.hasAccount() -> error = "Nenhuma conta neste aparelho. Crie uma conta primeiro."
                    vm.login(email, password) -> onDone()
                    else -> error = "E-mail ou senha incorretos."
                }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Não tem conta?", style = FocoText.Body14, color = FocoColor.TextSecondary)
                Text(
                    "Criar conta",
                    style = FocoText.Label14,
                    color = FocoColor.Primary,
                    modifier = Modifier.clickable(onClick = onGoSignUp),
                )
            }
        }
    }
}
