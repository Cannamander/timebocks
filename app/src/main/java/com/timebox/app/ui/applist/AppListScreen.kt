package com.timebox.app.ui.applist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.timebox.app.util.PackageIcon
import com.timebox.app.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onDone: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppListEvent.ShowTimePicker -> {
                    pickerTarget = event.packageName to event.appName
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search apps") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Choose Apps")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) searchQuery = ""
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onDone) {
                Text("Done")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.apps, key = { it.packageName }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = PackageIcon(item.packageName),
                                contentDescription = item.appName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = 12.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.appName)
                                Text(
                                    if (item.isLimited) {
                                        "${TimeUtils.formatDuration(item.dailyLimitMs)} / day"
                                    } else {
                                        "No limit"
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = item.isLimited,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.onAppSwitchChanged(
                                            item.packageName,
                                            item.appName,
                                            true
                                        )
                                    } else {
                                        val prevMs = item.dailyLimitMs
                                        val pkg = item.packageName
                                        val name = item.appName
                                        viewModel.onAppSwitchChanged(pkg, name, false)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Limit removed",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreLimit(pkg, name, prevMs)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pickerTarget?.let { (pkg, name) ->
        TimeLimitPickerDialog(
            appName = name,
            onDismiss = { pickerTarget = null },
            onConfirm = { ms ->
                viewModel.saveLimit(pkg, name, ms)
                pickerTarget = null
            }
        )
    }
}
