package com.example.ui.screen.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.designsystem.EmptyState
import com.example.ui.designsystem.LoadingState
import com.example.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllowedAppsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dockLimitMsg = stringResource(R.string.launcher_dock_limit_hint)
    val visibleApps = uiState.filteredAllowedManagerApps

    Scaffold(
        modifier = modifier.testTag("allowed_apps_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.launcher_allowed_apps_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("allowed_apps_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::applyEssentialPreset,
                        modifier = Modifier.testTag("allowed_presets_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.xxs))
                        Text(stringResource(R.string.launcher_presets_essentials))
                    }
                    if (uiState.allowedApps.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::clearAllAllowed,
                            modifier = Modifier.testTag("allowed_clear_button")
                        ) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.launcher_clear_all)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.launcher_allowed_apps_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.xxs)
                    )
                }

                item {
                    LauncherRulesCard(
                        uiState = uiState,
                        onToggleStrictFocus = viewModel::updateStrictFocusDrawer,
                        onToggleHideNonAllowed = viewModel::updateHideNonAllowedDuringFocus,
                        onToggleMinimalList = viewModel::updateMinimalHomeList,
                        onTogglePauseAllowed = viewModel::updatePauseOnAllowedApp
                    )
                }

                item {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(
                            R.string.launcher_allowed_summary_counts,
                            uiState.allowedApps.size,
                            uiState.allApps.count { it.isPinnedToDock }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.allowedQuery,
                        onValueChange = viewModel::updateAllowedQuery,
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.launcher_drawer_search_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.allowedQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateAllowedQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allowed_apps_search_input")
                    )
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        FilterChip(
                            selected = uiState.allowedFilter == AllowedFilter.ALL,
                            onClick = { viewModel.updateAllowedFilter(AllowedFilter.ALL) },
                            label = { Text(stringResource(R.string.launcher_drawer_filter_all, uiState.allApps.size)) }
                        )
                        FilterChip(
                            selected = uiState.allowedFilter == AllowedFilter.ALLOWED,
                            onClick = { viewModel.updateAllowedFilter(AllowedFilter.ALLOWED) },
                            label = { Text(stringResource(R.string.launcher_drawer_filter_allowed, uiState.allowedApps.size)) }
                        )
                        FilterChip(
                            selected = uiState.allowedFilter == AllowedFilter.PINNED,
                            onClick = { viewModel.updateAllowedFilter(AllowedFilter.PINNED) },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.launcher_filter_pinned,
                                        uiState.allApps.count { it.isPinnedToDock }
                                    )
                                )
                            }
                        )
                    }
                }

                when {
                    uiState.isLoadingApps -> {
                        item { LoadingState(Modifier.fillMaxWidth().height(220.dp)) }
                    }
                    visibleApps.isEmpty() -> {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.launcher_drawer_empty),
                                body = stringResource(R.string.launcher_allowed_empty_body),
                                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg)
                            )
                        }
                    }
                    else -> {
                        itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                            AllowedAppRow(
                                app = app,
                                showReorder = uiState.allowedFilter == AllowedFilter.ALLOWED && uiState.allowedQuery.isBlank(),
                                canMoveUp = index > 0,
                                canMoveDown = index < visibleApps.lastIndex,
                                onMoveUp = { viewModel.moveAllowed(app.packageName, -1) },
                                onMoveDown = { viewModel.moveAllowed(app.packageName, 1) },
                                onTogglePin = {
                                    viewModel.toggleDockPin(app) {
                                        scope.launch { snackbarHostState.showSnackbar(dockLimitMsg) }
                                    }
                                },
                                onToggleAllowed = { enabled -> viewModel.setAllowed(app, enabled) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherRulesCard(
    uiState: LauncherUiState,
    onToggleStrictFocus: (Boolean) -> Unit,
    onToggleHideNonAllowed: (Boolean) -> Unit,
    onToggleMinimalList: (Boolean) -> Unit,
    onTogglePauseAllowed: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            RuleSwitchRow(
                title = stringResource(R.string.launcher_settings_strict_focus),
                subtitle = stringResource(R.string.launcher_settings_strict_focus_body),
                checked = uiState.prefs.strictFocusDrawer,
                onCheckedChange = onToggleStrictFocus,
                testTag = "rule_strict_focus_switch"
            )
            HorizontalDivider()
            RuleSwitchRow(
                title = stringResource(R.string.launcher_settings_hide_non_allowed),
                subtitle = stringResource(R.string.launcher_settings_hide_non_allowed_body),
                checked = uiState.prefs.hideNonAllowedDuringFocus,
                onCheckedChange = onToggleHideNonAllowed,
                testTag = "rule_hide_non_allowed_switch"
            )
            HorizontalDivider()
            RuleSwitchRow(
                title = stringResource(R.string.launcher_settings_minimal_layout),
                subtitle = stringResource(R.string.launcher_settings_minimal_layout_body),
                checked = uiState.prefs.minimalHomeList,
                onCheckedChange = onToggleMinimalList,
                testTag = "rule_minimal_layout_switch"
            )
            HorizontalDivider()
            RuleSwitchRow(
                title = stringResource(R.string.launcher_settings_pause_on_allowed),
                subtitle = stringResource(R.string.launcher_settings_pause_on_allowed_body),
                checked = uiState.prefs.pauseOnAllowedApp,
                onCheckedChange = onTogglePauseAllowed,
                testTag = "rule_pause_on_allowed_switch"
            )
        }
    }
}

@Composable
private fun RuleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AllowedAppRow(
    app: LauncherAppItem,
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleAllowed: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = { AppIconAvatar(app = app, size = 40.dp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showReorder) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.launcher_action_move_up)
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.launcher_action_move_down)
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.testTag("pin_dock_${app.packageName}")
                ) {
                    Icon(
                        imageVector = if (app.isPinnedToDock) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(
                            if (app.isPinnedToDock) R.string.launcher_action_unpin_dock
                            else R.string.launcher_action_pin_dock
                        ),
                        tint = if (app.isPinnedToDock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = app.isAllowed,
                    onCheckedChange = onToggleAllowed,
                    modifier = Modifier.testTag("allowed_toggle_${app.packageName}")
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
