package com.foco.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.foco.app.AppViewModel
import com.foco.app.UiEvent
import com.foco.app.appViewModelFactory
import com.foco.app.ui.screens.FocusScreen
import com.foco.app.ui.screens.HomeScreen
import com.foco.app.ui.screens.LoginScreen
import com.foco.app.ui.screens.NewTaskScreen
import com.foco.app.ui.screens.OnboardingScreen
import com.foco.app.ui.screens.ProfileScreen
import com.foco.app.ui.screens.SessionDoneScreen
import com.foco.app.ui.screens.SignUpScreen
import com.foco.app.ui.screens.StatsScreen

object Routes {
    const val Onboarding = "onboarding"
    const val SignUp = "signup"
    const val Login = "login"
    const val Home = "home"
    const val NewTask = "newtask"
    const val Focus = "focus"
    const val Done = "done"
    const val Stats = "stats"
    const val Profile = "profile"
}

private val tabRoutes = setOf(Routes.Home, Routes.Stats, Routes.Focus, Routes.Profile)

@Composable
fun FocoApp() {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = remember(context) { appViewModelFactory(context) })
    FocoApp(vm)
}

/** Versão que recebe o [AppViewModel] pronto; os testes de tela usam um ViewModel com dados falsos. */
@Composable
fun FocoApp(vm: AppViewModel) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val start = remember { if (vm.profile.value.loggedIn) Routes.Home else Routes.Onboarding }

    // eventos do timer: abre a tela "Sessão concluída" ou avisa que a pausa terminou
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                UiEvent.SessionFinished -> nav.navigate(Routes.Done) { launchSingleTop = true }
                UiEvent.BreakFinished ->
                    Toast.makeText(context, "Pausa encerrada. Hora de focar!", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun goHome() {
        nav.navigate(Routes.Home) {
            popUpTo(Routes.Home) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun goToWelcome() {
        nav.navigate(Routes.Onboarding) {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    Column(Modifier.fillMaxSize().background(FocoColor.Background)) {
        Box(Modifier.weight(1f)) {
            NavHost(navController = nav, startDestination = start) {
                composable(Routes.Onboarding) {
                    OnboardingScreen(
                        onCreateAccount = { nav.navigate(Routes.SignUp) },
                        onLogin = { nav.navigate(Routes.Login) },
                    )
                }
                composable(Routes.SignUp) {
                    SignUpScreen(
                        vm = vm,
                        onDone = {
                            nav.navigate(Routes.Home) { popUpTo(Routes.Onboarding) { inclusive = true } }
                        },
                        onBack = { nav.popBackStack() },
                        onGoLogin = { nav.navigate(Routes.Login) { popUpTo(Routes.SignUp) { inclusive = true } } },
                    )
                }
                composable(Routes.Login) {
                    LoginScreen(
                        vm = vm,
                        onDone = {
                            nav.navigate(Routes.Home) { popUpTo(Routes.Onboarding) { inclusive = true } }
                        },
                        onBack = { nav.popBackStack() },
                        onGoSignUp = { nav.navigate(Routes.SignUp) { popUpTo(Routes.Login) { inclusive = true } } },
                    )
                }
                composable(Routes.Home) {
                    HomeScreen(
                        vm = vm,
                        onOpenTask = { id ->
                            vm.focusOn(id)
                            nav.navigate(Routes.Focus) {
                                popUpTo(Routes.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Routes.NewTask) { NewTaskScreen(vm = vm, onClose = { nav.popBackStack() }) }
                composable(Routes.Focus) { FocusScreen(vm = vm) }
                composable(Routes.Done) {
                    SessionDoneScreen(
                        vm = vm,
                        onHome = { goHome() },
                        onBreak = {
                            nav.navigate(Routes.Focus) {
                                popUpTo(Routes.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Routes.Stats) { StatsScreen(vm = vm) }
                composable(Routes.Profile) { ProfileScreen(vm = vm, onLoggedOut = { goToWelcome() }) }
            }
        }

        if (route in tabRoutes) {
            FocoBottomBar(
                currentRoute = route,
                onTab = { tab ->
                    nav.navigate(tab.route) {
                        popUpTo(Routes.Home) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAdd = { nav.navigate(Routes.NewTask) },
            )
        }
    }
}
