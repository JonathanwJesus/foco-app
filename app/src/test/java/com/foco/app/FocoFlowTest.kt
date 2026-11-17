package com.foco.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.foco.app.data.FocusSession
import com.foco.app.data.Profile
import com.foco.app.data.Subject
import com.foco.app.data.Task
import com.foco.app.testing.FakeReminderScheduler
import com.foco.app.testing.FakeTimerAlarm
import com.foco.app.testing.InMemoryRepository
import com.foco.app.testing.task
import com.foco.app.ui.FocoApp
import com.foco.app.ui.FocoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Testes de tela: abrem o app inteiro (navegação incluída) com um ViewModel de dados falsos e
 * fazem o que um usuário faria. Rodam na JVM com o Robolectric, sem emulador.
 */
@RunWith(RobolectricTestRunner::class)
// O Android 36 ou superior exige Java 21 no Robolectric; o 35 roda com o Java 17.
// A tela padrão do Robolectric é minúscula (320 x 470 dp); aqui usamos o tamanho de um celular comum.
@Config(sdk = [35], qualifiers = "w412dp-h915dp-normal-long-notround-port-notnight-xxhdpi")
class FocoFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun newVm(repo: InMemoryRepository = InMemoryRepository()) =
        AppViewModel(repo, FakeReminderScheduler(), FakeTimerAlarm())

    private fun launch(vm: AppViewModel) {
        compose.setContent { FocoTheme { FocoApp(vm) } }
    }

    /** ViewModel com o usuário já logado, para o teste começar direto na tela Hoje. */
    private fun loggedInVm(tasks: List<Task> = emptyList(), sessions: List<FocusSession> = emptyList()) =
        newVm(
            InMemoryRepository(
                tasks = tasks,
                sessions = sessions,
                profile = Profile(
                    name = "Ana Souza",
                    email = "ana@email.com",
                    passwordHash = "hash-de-teste",
                    loggedIn = true,
                ),
            )
        )

    private fun taskToday(title: String = "Resolver lista de derivadas") =
        task(id = 1, title = title, day = LocalDate.now())

    // ------------------------------------------------------------------
    // Cadastro e login
    // ------------------------------------------------------------------

    @Test
    fun cadastro_vazio_mostra_os_tres_erros() {
        launch(newVm())

        compose.onNodeWithText("Criar minha conta").performClick()
        compose.onAllNodesWithText("Criar conta").onLast().performClick()

        compose.onNodeWithText("Informe seu nome").assertIsDisplayed()
        compose.onNodeWithText("Digite um e-mail válido").assertIsDisplayed()
        compose.onNodeWithText("A senha precisa ter pelo menos 8 caracteres").assertIsDisplayed()
    }

    @Test
    fun cadastro_valido_abre_a_tela_hoje() {
        launch(newVm())

        compose.onNodeWithText("Criar minha conta").performClick()
        val campos = compose.onAllNodes(hasSetTextAction())
        campos[0].performTextInput("Ana Souza")
        campos[1].performTextInput("ana@email.com")
        campos[2].performTextInput("senha1234")
        compose.onAllNodesWithText("Criar conta").onLast().performClick()

        compose.onNodeWithText("Olá, Ana").assertIsDisplayed()
    }

    @Test
    fun login_com_senha_errada_mostra_erro_e_com_a_certa_entra() {
        val vm = newVm()
        vm.signUp("Ana Souza", "ana@email.com", "senha1234")
        vm.logout()
        launch(vm)

        compose.onNodeWithText("Já tenho conta").performClick()
        val campos = compose.onAllNodes(hasSetTextAction())
        campos[0].performTextInput("ana@email.com")
        campos[1].performTextInput("senhaerrada")
        compose.onAllNodesWithText("Entrar").onLast().performClick()
        compose.onNodeWithText("E-mail ou senha incorretos.").assertIsDisplayed()

        campos[1].performTextReplacement("senha1234")
        compose.onAllNodesWithText("Entrar").onLast().performClick()
        compose.onNodeWithText("Olá, Ana").assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // Tarefas
    // ------------------------------------------------------------------

    @Test
    fun hoje_sem_tarefas_mostra_o_convite_para_criar() {
        launch(loggedInVm())

        compose.onNodeWithText("Nenhuma tarefa ainda").assertIsDisplayed()
    }

    @Test
    fun criar_tarefa_pelo_botao_mais_e_ve_la_na_lista() {
        launch(loggedInVm())

        compose.onNodeWithContentDescription("Nova tarefa").performClick()
        compose.onNodeWithText("Salvar tarefa").assertIsNotEnabled() // sem título, o botão fica desabilitado

        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Estudar integrais")
        compose.onNodeWithText("Salvar tarefa").performClick()

        compose.onNodeWithText("Estudar integrais").assertIsDisplayed()
        compose.onNodeWithText("1 pendentes").assertIsDisplayed()
    }

    @Test
    fun concluir_tarefa_leva_para_a_secao_de_concluidas() {
        launch(loggedInVm(tasks = listOf(taskToday())))

        compose.onNodeWithContentDescription("Concluir tarefa").performClick()

        compose.onNodeWithText("Concluídas hoje · 1").assertIsDisplayed()
        compose.onNodeWithText("0 pendentes").assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // Modo foco
    // ------------------------------------------------------------------

    @Test
    fun sessao_de_foco_do_inicio_ao_fim() {
        launch(loggedInVm(tasks = listOf(taskToday("Resolver lista"))))

        // toca na tarefa: abre o modo foco já com ela escolhida
        compose.onNodeWithText("Resolver lista").performClick()
        compose.onNodeWithText("Modo foco").assertIsDisplayed()
        compose.onNodeWithText("25:00").assertIsDisplayed()
        compose.onNodeWithText("Resolver lista · Cálculo II").assertIsDisplayed()

        // atalho de demonstração: conclui a sessão na hora
        compose.onNodeWithText("Pular para o fim (demonstração)").performClick()
        compose.onNodeWithText("Sessão concluída!").assertIsDisplayed()
        compose.onNodeWithText("Você focou por 25 minutos em Cálculo II. Ótimo trabalho, continue assim!")
            .assertIsDisplayed()

        // marca a tarefa como feita e volta para a tela Hoje
        compose.onNodeWithText("Marcar tarefa como feita").performClick()
        compose.onNodeWithText("Concluídas hoje · 1").assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // Estatísticas
    // ------------------------------------------------------------------

    @Test
    fun estatisticas_somam_as_sessoes_do_periodo() {
        val agora = System.currentTimeMillis()
        val sessoes = listOf(
            FocusSession(agora - 2_000, Subject.CALCULO, null, 25, agora - 2_000),
            FocusSession(agora - 1_000, Subject.FISICA, null, 25, agora - 1_000),
        )
        launch(loggedInVm(sessions = sessoes))

        compose.onNodeWithText("Estatísticas").performClick()

        compose.onNodeWithText("Tempo de foco na semana").assertIsDisplayed()
        compose.onNodeWithText("Por disciplina").assertIsDisplayed()
    }
}
