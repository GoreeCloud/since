package com.goreecloud.since.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goreecloud.since.R

@Composable
fun SinceApp() {
    var showDevelopmentBoundary by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDevelopmentBoundary = true },
                content = { Text(stringResource(R.string.add_tracker)) },
            )
        },
    ) { innerPadding ->
        DashboardEmptyState(innerPadding)
    }

    if (showDevelopmentBoundary) {
        AlertDialog(
            onDismissRequest = { showDevelopmentBoundary = false },
            title = { Text(stringResource(R.string.development_boundary_title)) },
            text = { Text(stringResource(R.string.development_boundary_message)) },
            confirmButton = {
                TextButton(onClick = { showDevelopmentBoundary = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun DashboardEmptyState(
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(R.string.dashboard_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.dashboard_empty_status),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
