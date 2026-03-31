package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.repository.QosScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpsertQosScriptUseCaseTest {

    @Test
    fun `upsert validates and persists script with families`() = runTest {
        val repository = FakeQosScriptRepository()
        val useCase = UpsertQosScriptUseCase(repository)

        val saved = useCase(
            id = null,
            name = " Script QoS 4G ",
            repeatCount = 2,
            targetTechnologies = setOf(" 4G ", "5G"),
            testFamilies = setOf(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL)
        )

        assertThat(saved.name).isEqualTo("Script QoS 4G")
        assertThat(saved.repeatCount).isEqualTo(2)
        assertThat(saved.targetTechnologies).containsExactly("4G", "5G")
        assertThat(saved.testFamilies).containsExactly(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL)
        assertThat(repository.lastUpserted).isNotNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsert rejects empty family set`() = runTest {
        val useCase = UpsertQosScriptUseCase(FakeQosScriptRepository())
        useCase(
            id = null,
            name = "Script",
            repeatCount = 1,
            targetTechnologies = emptySet(),
            testFamilies = emptySet()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsert rejects empty technology set when families are selected`() = runTest {
        val useCase = UpsertQosScriptUseCase(FakeQosScriptRepository())
        useCase(
            id = null,
            name = "Script QoS",
            repeatCount = 1,
            targetTechnologies = emptySet(),
            testFamilies = setOf(QosTestFamily.SMS)
        )
    }
}

private class FakeQosScriptRepository : QosScriptRepository {
    var lastUpserted: QosScriptDefinition? = null

    override fun observeActiveScripts(): Flow<List<QosScriptDefinition>> = flowOf(emptyList())

    override suspend fun getById(scriptId: String): QosScriptDefinition? = null

    override suspend fun upsert(script: QosScriptDefinition): QosScriptDefinition {
        lastUpserted = script
        return script
    }

    override suspend fun archive(scriptId: String) = Unit
}
