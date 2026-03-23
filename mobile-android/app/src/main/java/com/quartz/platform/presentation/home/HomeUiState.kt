package com.quartz.platform.presentation.home

import com.quartz.platform.domain.model.NetworkStatus

data class HomeUiState(
    val siteCount: Int = 0,
    val pendingSyncJobs: Int = 0,
    val networkStatus: NetworkStatus = NetworkStatus.UNAVAILABLE,
    val keyOperationalFlows: List<String> = listOf(
        "Inspection site (secteurs, antennes, cellules)",
        "Session XFeeder / MixFeeder",
        "Débit et latence",
        "Validation RET",
        "Exécution script QoS",
        "Suivi et envoi de rapports"
    )
)
