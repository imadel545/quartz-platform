package com.quartz.platform.presentation.site.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.SiteSummary

@Composable
fun SiteListRoute(
    onSiteSelected: (String) -> Unit,
    viewModel: SiteListViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    SiteListScreen(
        state = state,
        onSiteSelected = onSiteSelected,
        onLoadDemoSnapshot = viewModel::onLoadDemoSnapshotClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteListScreen(
    state: SiteListUiState,
    onSiteSelected: (String) -> Unit,
    onLoadDemoSnapshot: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_sites_quartz)) }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = onLoadDemoSnapshot,
                        enabled = !state.isBootstrappingDemo
                    ) {
                        Text(
                            if (state.isBootstrappingDemo) {
                                stringResource(R.string.action_load_demo_snapshot_loading)
                            } else {
                                stringResource(R.string.action_load_demo_snapshot)
                            }
                        )
                    }
                }
            }

            state.isEmpty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.empty_site_snapshot_cache),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.empty_site_snapshot_cache_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onLoadDemoSnapshot,
                        enabled = !state.isBootstrappingDemo
                    ) {
                        Text(
                            if (state.isBootstrappingDemo) {
                                stringResource(R.string.action_load_demo_snapshot_loading)
                            } else {
                                stringResource(R.string.action_load_demo_snapshot)
                            }
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.infoMessage?.let { info ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = info,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(state.sites, key = { it.id }) { site ->
                        SiteSummaryCard(
                            site = site,
                            onClick = { onSiteSelected(site.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteSummaryCard(
    site: SiteSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = site.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.label_site_code, site.externalCode),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.label_site_sectors_summary,
                    site.sectorsInService,
                    site.sectorsForecast
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (site.indoorOnly) {
                    stringResource(R.string.label_site_type_indoor)
                } else {
                    stringResource(R.string.label_site_type_indoor_outdoor)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
