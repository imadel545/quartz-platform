package com.quartz.platform.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quartz.platform.domain.model.NetworkStatus

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    HomeScreen(state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Quartz Field Operations")
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatusCard(
                    siteCount = state.siteCount,
                    pendingSyncJobs = state.pendingSyncJobs,
                    networkStatusLabel = if (state.networkStatus == NetworkStatus.AVAILABLE) {
                        "Disponible"
                    } else {
                        "Indisponible"
                    }
                )
            }

            item {
                FieldReadinessCard(siteCount = state.siteCount)
            }

            item {
                Text(
                    text = "Flux opérationnels terrain",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(state.keyOperationalFlows) { flowName ->
                AssistChip(
                    onClick = {},
                    label = { Text(flowName) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    siteCount: Int,
    pendingSyncJobs: Int,
    networkStatusLabel: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Sites en cache: $siteCount", style = MaterialTheme.typography.bodyLarge)
            Text("Jobs sync en attente: $pendingSyncJobs", style = MaterialTheme.typography.bodyLarge)
            Text("Réseau: $networkStatusLabel", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FieldReadinessCard(siteCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("État baseline", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (siteCount == 0) {
                    "Aucun site n'est en cache local pour le moment."
                } else {
                    "Cache local disponible pour la navigation des sites."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
