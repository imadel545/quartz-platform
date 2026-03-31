package com.quartz.platform.domain.model

data class QosScriptDefinition(
    val id: String,
    val name: String,
    val repeatCount: Int,
    val targetTechnologies: Set<String>,
    val testFamilies: Set<QosTestFamily>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isArchived: Boolean = false
)

object QosDefaultScripts {
    fun build(nowEpochMillis: Long): List<QosScriptDefinition> {
        return listOf(
            QosScriptDefinition(
                id = "qos-script-latency-throughput",
                name = "Latence + Débit",
                repeatCount = 1,
                targetTechnologies = setOf("4G", "5G"),
                testFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY),
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            ),
            QosScriptDefinition(
                id = "qos-script-voice-sms",
                name = "Voix / SMS",
                repeatCount = 1,
                targetTechnologies = setOf("4G", "5G"),
                testFamilies = setOf(
                    QosTestFamily.SMS,
                    QosTestFamily.VOLTE_CALL,
                    QosTestFamily.CSFB_CALL,
                    QosTestFamily.EMERGENCY_CALL,
                    QosTestFamily.STANDARD_CALL
                ),
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            ),
            QosScriptDefinition(
                id = "qos-script-video-streaming",
                name = "Streaming vidéo",
                repeatCount = 1,
                targetTechnologies = setOf("4G", "5G"),
                testFamilies = setOf(QosTestFamily.VIDEO_STREAMING),
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            )
        )
    }
}
