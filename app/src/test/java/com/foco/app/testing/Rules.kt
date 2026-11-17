package com.foco.app.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.TimeZone

/** Troca o dispatcher "Main" por um de teste, para o viewModelScope rodar na JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Fixa o fuso horário em UTC, para os testes darem o mesmo resultado em qualquer computador. */
class UtcTimeZoneRule : TestWatcher() {
    private var previous: TimeZone? = null

    override fun starting(description: Description) {
        previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    override fun finished(description: Description) {
        previous?.let { TimeZone.setDefault(it) }
    }
}
